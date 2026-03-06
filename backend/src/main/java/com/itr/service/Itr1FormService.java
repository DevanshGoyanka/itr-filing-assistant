package com.itr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr1FormData.*;
import com.itr.entity.Client;
import com.itr.entity.ClientYearData;
import com.itr.exception.BusinessLogicException;
import com.itr.exception.ResourceNotFoundException;
import com.itr.repository.ClientYearDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ITR-1 Form Service — manages form lifecycle, computation, validation.
 * CBDT AY 2025-26 Validation Rules V1.1 compliant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Itr1FormService {

    private final ClientYearDataRepository yearDataRepository;
    private final ClientService clientService;
    private final TaxComputationService taxService;
    private final Itr1ValidationService validationService;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────
    // GET form data — auto-populate from prefill if available
    // ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Itr1FormData getFormData(Long clientId, String year, Long userId) {
        Client client = clientService.getClientEntity(clientId, userId);
        ClientYearData yearData = yearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No data found for " + year + ". Please upload prefill first."));

        // If we have a saved computed ITR1 JSON with full form, parse it
        if (yearData.getComputedItr1Json() != null && isFullFormData(yearData.getComputedItr1Json())) {
            try {
                return objectMapper.readValue(yearData.getComputedItr1Json(), Itr1FormData.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse saved form data, rebuilding from prefill", e);
            }
        }

        // Build from prefill data
        return buildFromPrefill(client, yearData);
    }

    // ─────────────────────────────────────────────────────────
    // SAVE form data (user edits)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public Itr1FormData saveFormData(Long clientId, String year, Itr1FormData formData, Long userId) {
        Client client = clientService.getClientEntity(clientId, userId);
        ClientYearData yearData = yearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseThrow(() -> new ResourceNotFoundException("No data found for " + year));

        try {
            yearData.setComputedItr1Json(objectMapper.writeValueAsString(formData));
            yearData.setStatus("draft");
            yearDataRepository.save(yearData);
        } catch (JsonProcessingException e) {
            throw new BusinessLogicException("Failed to save form data");
        }

        return formData;
    }

    // ─────────────────────────────────────────────────────────
    // COMPUTE — full recalculation with CBDT rules
    // ─────────────────────────────────────────────────────────
    @Transactional
    public Itr1FormData computeForm(Long clientId, String year, Itr1FormData formData, Long userId) {
        Client client = clientService.getClientEntity(clientId, userId);
        ClientYearData yearData = yearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseThrow(() -> new ResourceNotFoundException("No data found for " + year));

        boolean isNewRegime = formData.getPartA() != null && formData.getPartA().isNewTaxRegime();
        String natureOfEmployment = formData.getPartA() != null ?
                formData.getPartA().getNatureOfEmployment() : "others";

        // ───── 1. Compute Schedule Salary [Rules 59-62, 112, 224] ─────
        computeSalary(formData, isNewRegime, natureOfEmployment);

        // ───── 2. Compute Schedule HP [Rules 43-49, 161, 163, 263] ─────
        computeHouseProperty(formData, isNewRegime);

        // ───── 3. Compute Schedule OS [Rules 50-56, 223] ─────
        computeOtherSources(formData, isNewRegime);

        // ───── 4. Compute Exempt Income ─────
        computeExemptIncome(formData);

        // ───── 5. Compute GTI ─────
        double gti = (formData.getScheduleSalary() != null ? formData.getScheduleSalary().getIncomeFromSalary() : 0)
                + (formData.getScheduleHP() != null ? formData.getScheduleHP().getIncomeFromHP() : 0)
                + (formData.getScheduleOS() != null ? formData.getScheduleOS().getIncomeFromOtherSources() : 0);

        // ───── 6. Compute Deductions VI-A [Rules 115-176] ─────
        computeDeductions(formData, isNewRegime, gti, natureOfEmployment);

        double totalDeductions = formData.getDeductionsVIA() != null ?
                formData.getDeductionsVIA().getTotalDeductions() : 0;

        // ───── 7. Tax Computation ─────
        TaxComputation computation = taxService.computeTax(gti, totalDeductions, isNewRegime);

        // ───── 8. Taxes Paid ─────
        computeTaxesPaid(formData);
        double totalTaxesPaid = formData.getTaxesPaid() != null ?
                formData.getTaxesPaid().getTotalTaxesPaid() : 0;

        computation.setTotalTaxesPaid(totalTaxesPaid);
        double balancePayable = computation.getTotalTaxLiability() - totalTaxesPaid;
        if (balancePayable >= 0) {
            computation.setBalanceTaxPayable(balancePayable);
            computation.setRefundDue(0);
        } else {
            computation.setBalanceTaxPayable(0);
            computation.setRefundDue(Math.abs(balancePayable));
        }

        formData.setComputation(computation);

        // ───── 9. Validate ─────
        List<String> errors = validationService.validate(formData);
        List<String> warnings = validationService.getWarnings(formData);
        formData.setValidationErrors(errors);
        formData.setValidationWarnings(warnings);

        // ───── 10. Save ─────
        try {
            yearData.setComputedItr1Json(objectMapper.writeValueAsString(formData));
            yearData.setStatus(errors.isEmpty() ? "computed" : "draft");
            yearDataRepository.save(yearData);
        } catch (JsonProcessingException e) {
            throw new BusinessLogicException("Failed to save computed form data");
        }

        return formData;
    }

    // ─────────────────────────────────────────────────────────
    // FINALIZE — generate downloadable ITR-1 JSON
    // ─────────────────────────────────────────────────────────
    @Transactional
    public String generateItr1Json(Long clientId, String year, Long userId) {
        Client client = clientService.getClientEntity(clientId, userId);
        ClientYearData yearData = yearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseThrow(() -> new ResourceNotFoundException("No data found for " + year));

        if (yearData.getComputedItr1Json() == null) {
            throw new BusinessLogicException("Please compute the form first before downloading");
        }

        Itr1FormData formData;
        try {
            formData = objectMapper.readValue(yearData.getComputedItr1Json(), Itr1FormData.class);
        } catch (JsonProcessingException e) {
            throw new BusinessLogicException("Form data is corrupted. Please recompute.");
        }

        // Validate before finalization
        List<String> errors = validationService.validate(formData);
        if (!errors.isEmpty()) {
            throw new BusinessLogicException("Form has validation errors: " + String.join("; ", errors));
        }

        yearData.setStatus("filed");
        yearDataRepository.save(yearData);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formData);
        } catch (JsonProcessingException e) {
            throw new BusinessLogicException("Failed to generate ITR-1 JSON");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Computation Methods
    // ═══════════════════════════════════════════════════════════

    private void computeSalary(Itr1FormData formData, boolean isNewRegime, String natureOfEmployment) {
        ScheduleSalary sal = formData.getScheduleSalary();
        if (sal == null) {
            sal = ScheduleSalary.builder().build();
            formData.setScheduleSalary(sal);
        }

        // Rule 59: Gross Salary = 17(1) + 17(2) + 17(3) + 89A(notified) + 89A(other)
        double gross = sal.getSalaryU17_1() + sal.getPerquisitesU17_2() + sal.getProfitsU17_3()
                + sal.getIncomeU89A_notified() + sal.getIncomeU89A_other();
        sal.setGrossSalary(gross);

        // Rule 63: Total exempt u/s 10 cannot exceed gross salary
        double exempt = Math.min(sal.getAllowancesExemptU10(), gross);
        sal.setAllowancesExemptU10(exempt);

        // Rule 60: Net Salary = Gross - Exempt - Relief89A
        double net = gross - exempt - sal.getReliefU89A();
        sal.setNetSalary(Math.max(net, 0));

        // Standard deduction u/s 16(ia) [Rule 112 old = 50000, Rule 224 new = 75000]
        double maxStdDed = isNewRegime ? 75000 : 50000;
        // Cannot exceed net salary
        sal.setStandardDeduction(Math.min(maxStdDed, sal.getNetSalary()));

        // Entertainment allowance u/s 16(ii) [Rules 57,58,164]
        if (isNewRegime) {
            sal.setEntertainmentAllowance(0); // Rule 164: Not allowed in new regime
        } else {
            // Only for CG/SG/PSU employees
            if (!"CG".equals(natureOfEmployment) && !"SG".equals(natureOfEmployment)
                    && !"PSU".equals(natureOfEmployment)) {
                sal.setEntertainmentAllowance(0); // Rule 58
            }
        }

        // Professional tax u/s 16(iii) [Rule 169]
        if (isNewRegime) {
            sal.setProfessionalTax(0); // Not allowed in new regime
        }

        // Rule 61: Total deductions u/s 16
        double dedU16 = sal.getStandardDeduction() + sal.getEntertainmentAllowance()
                + sal.getProfessionalTax();
        sal.setTotalDeductionsU16(dedU16);

        // Rule 62: Income from salary
        sal.setIncomeFromSalary(Math.max(sal.getNetSalary() - dedU16, 0));
    }

    private void computeHouseProperty(Itr1FormData formData, boolean isNewRegime) {
        ScheduleHouseProperty hp = formData.getScheduleHP();
        if (hp == null) {
            hp = ScheduleHouseProperty.builder().build();
            formData.setScheduleHP(hp);
        }

        boolean isSelfOccupied = "self_occupied".equals(hp.getPropertyType());

        if (isSelfOccupied) {
            // Rule 44/250: Annual value of self-occupied = 0
            hp.setGrossRent(0);
            hp.setMunicipalTaxPaid(0); // Rule 49: Municipal tax not allowed for self-occupied
            hp.setAnnualValue(0);
            hp.setStandardDeduction30Pct(0);

            if (isNewRegime) {
                // Rule 163/263: Interest on self-occupied not allowed in new regime
                hp.setInterestOnLoanU24b(0);
            } else {
                // Rule 48: Self-occupied interest max 200000
                hp.setInterestOnLoanU24b(Math.min(hp.getInterestOnLoanU24b(), 200000));
            }

            hp.setIncomeFromHP(0 - hp.getInterestOnLoanU24b()); // Loss from HP
        } else {
            // Let out / Deemed let out
            // Rule 46: Annual Value = Gross Rent - Municipal Tax
            double annualValue = hp.getGrossRent() - hp.getMunicipalTaxPaid();
            hp.setAnnualValue(Math.max(annualValue, 0));

            // Rule 43: Standard deduction = 30% of annual value
            double stdDed = hp.getAnnualValue() * 0.30;
            hp.setStandardDeduction30Pct(stdDed);

            // Rule 47: Income from HP = AnnualValue - StdDed - Interest + Arrears
            double incHP = hp.getAnnualValue() - stdDed - hp.getInterestOnLoanU24b()
                    + hp.getArrearsUnrealizedRent();
            hp.setIncomeFromHP(incHP);
        }
    }

    private void computeOtherSources(Itr1FormData formData, boolean isNewRegime) {
        ScheduleOtherSources os = formData.getScheduleOS();
        if (os == null) {
            os = ScheduleOtherSources.builder().build();
            formData.setScheduleOS(os);
        }

        // Rule 52: Gross = sum of all OS income
        double gross = os.getSavingsInterest() + os.getDepositInterest()
                + os.getIncomeTaxRefundInterest() + os.getFamilyPension()
                + os.getDividendIncome() + os.getOtherIncome() + os.getIncomeU89A();
        os.setGrossOtherSources(gross);

        // Family pension deduction u/s 57(iia)
        if (os.getFamilyPension() > 0) {
            double oneThird = os.getFamilyPension() / 3.0;
            double maxDed = isNewRegime ? 25000 : 15000; // Rule 223 (new) / Rule 54 (old)
            os.setDeductionU57iia(Math.min(oneThird, maxDed));
        } else {
            os.setDeductionU57iia(0);
        }

        // Rule 56: Net OS = Gross - Deduction 57(iia)
        // Rule 53: Income from OS cannot be less than family pension deduction excess
        os.setIncomeFromOtherSources(gross - os.getDeductionU57iia());
    }

    private void computeExemptIncome(Itr1FormData formData) {
        ScheduleExemptIncome exempt = formData.getScheduleExempt();
        if (exempt == null) {
            exempt = ScheduleExemptIncome.builder().build();
            formData.setScheduleExempt(exempt);
        }

        // Rule 226: LTCG u/s 112A exemption max 125000
        if (exempt.getLtcgU112A_exempt() > 125000) {
            exempt.setLtcgU112A_exempt(125000);
        }

        exempt.setTotalExemptIncome(
                exempt.getAgricultureIncome() + exempt.getExemptInterestIncome()
                        + exempt.getLtcgU112A_exempt() + exempt.getOtherExemptIncome()
        );
    }

    private void computeDeductions(Itr1FormData formData, boolean isNewRegime,
                                    double gti, String natureOfEmployment) {
        DeductionsVIA ded = formData.getDeductionsVIA();
        if (ded == null) {
            ded = DeductionsVIA.builder().build();
            formData.setDeductionsVIA(ded);
        }

        if (isNewRegime) {
            // ── New Regime: Only 80CCD(2) allowed [Rules 147,154-160,170-176] ──
            double ccd2 = ded.getSection80CCD2();
            // Rule 225: 80CCD(2) max 14% of salary for CG/SG employees
            if ("CG".equals(natureOfEmployment) || "SG".equals(natureOfEmployment)) {
                double salaryForCCD2 = formData.getScheduleSalary() != null ?
                        formData.getScheduleSalary().getSalaryU17_1() : 0;
                ccd2 = Math.min(ccd2, salaryForCCD2 * 0.14);
            }

            // Zero out everything except 80CCD(2)
            ded.setSection80C(0);
            ded.setSection80CCC(0);
            ded.setSection80CCD1(0);
            ded.setTotal80C_CCC_CCD1(0);
            ded.setSection80CCD1B(0);
            ded.setSection80CCD2(ccd2);
            ded.setSection80D(0);
            ded.setSection80DD(0);
            ded.setSection80DDB(0);
            ded.setSection80E(0);
            ded.setSection80EE(0);
            ded.setSection80EEA(0);
            ded.setSection80EEB(0);
            ded.setSection80G(0);
            ded.setSection80GG(0);
            ded.setSection80GGA(0);
            ded.setSection80GGC(0);
            ded.setSection80TTA(0);
            ded.setSection80TTB(0);
            ded.setSection80U(0);
            ded.setSection80CCH(0);
            ded.setTotalDeductions(ccd2);
        } else {
            // ── Old Regime: Apply individual caps ──

            // 80C + 80CCC + 80CCD(1) combined max 150000
            double combined = ded.getSection80C() + ded.getSection80CCC() + ded.getSection80CCD1();
            ded.setTotal80C_CCC_CCD1(Math.min(combined, 150000));

            // 80CCD(1B) max 50000 [Rule 115]
            ded.setSection80CCD1B(Math.min(ded.getSection80CCD1B(), 50000));

            // 80CCD(2) max 14% of salary [Rule 121]
            double salaryForCCD2 = formData.getScheduleSalary() != null ?
                    formData.getScheduleSalary().getSalaryU17_1() : 0;
            if ("CG".equals(natureOfEmployment) || "SG".equals(natureOfEmployment)) {
                ded.setSection80CCD2(Math.min(ded.getSection80CCD2(), salaryForCCD2 * 0.14));
            } else {
                ded.setSection80CCD2(Math.min(ded.getSection80CCD2(), salaryForCCD2 * 0.10));
            }

            // 80EE max 50000 [Rule 122]
            ded.setSection80EE(Math.min(ded.getSection80EE(), 50000));

            // 80EEA max 150000 [Rule 123]
            ded.setSection80EEA(Math.min(ded.getSection80EEA(), 150000));

            // Rule 124: Only one of 80EE/80EEA allowed
            if (ded.getSection80EE() > 0 && ded.getSection80EEA() > 0) {
                // Keep the larger one
                if (ded.getSection80EEA() >= ded.getSection80EE()) {
                    ded.setSection80EE(0);
                } else {
                    ded.setSection80EEA(0);
                }
            }

            // 80EEB max 150000 [Rule 125]
            ded.setSection80EEB(Math.min(ded.getSection80EEB(), 150000));

            // 80GG max 60000 [Rule 114]
            ded.setSection80GG(Math.min(ded.getSection80GG(), 60000));

            // 80TTA max 10000
            ded.setSection80TTA(Math.min(ded.getSection80TTA(), 10000));

            // 80TTB max 50000
            ded.setSection80TTB(Math.min(ded.getSection80TTB(), 50000));

            // 80U: 75000 or 125000 [Rules 209-210]
            if (ded.getSection80U() > 0) {
                // If > 75000 then severe disability allowed max 125000
                ded.setSection80U(ded.getSection80U() > 75000 ?
                        Math.min(ded.getSection80U(), 125000) :
                        Math.min(ded.getSection80U(), 75000));
            }

            // Sum total
            double total = ded.getTotal80C_CCC_CCD1() + ded.getSection80CCD1B()
                    + ded.getSection80CCD2() + ded.getSection80D()
                    + ded.getSection80DD() + ded.getSection80DDB()
                    + ded.getSection80E() + ded.getSection80EE()
                    + ded.getSection80EEA() + ded.getSection80EEB()
                    + ded.getSection80G() + ded.getSection80GG()
                    + ded.getSection80GGA() + ded.getSection80GGC()
                    + ded.getSection80TTA() + ded.getSection80TTB()
                    + ded.getSection80U() + ded.getSection80CCH();

            // Total deductions cannot exceed GTI
            ded.setTotalDeductions(Math.min(total, gti));
        }
    }

    private void computeTaxesPaid(Itr1FormData formData) {
        ScheduleTaxesPaid tp = formData.getTaxesPaid();
        if (tp == null) {
            tp = ScheduleTaxesPaid.builder().build();
            formData.setTaxesPaid(tp);
        }

        // Rule 104: Total taxes paid
        tp.setTotalTaxesPaid(
                tp.getTdsOnSalary() + tp.getTdsOtherThanSalary() + tp.getTds3()
                        + tp.getTcs() + tp.getAdvanceTax() + tp.getSelfAssessmentTax()
        );
    }

    // ─────────────────────────────────────────────────────────
    // Build from prefill
    // ─────────────────────────────────────────────────────────
    private Itr1FormData buildFromPrefill(Client client, ClientYearData yearData) {
        JsonNode prefillJson = null;
        if (yearData.getRawPrefillJson() != null) {
            try {
                prefillJson = objectMapper.readTree(yearData.getRawPrefillJson());
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse prefill JSON", e);
            }
        }

        PartA_GeneralInfo partA = PartA_GeneralInfo.builder()
                .assesseeName(client.getName())
                .pan(client.getPan())
                .aadhaar(client.getAadhaar() != null ? client.getAadhaar() : "")
                .dob(client.getDob() != null ? client.getDob().toString() : "")
                .email(client.getEmail() != null ? client.getEmail() : "")
                .mobile(client.getMobile() != null ? client.getMobile() : "")
                .assessmentYear("AY2025-26")
                .newTaxRegime(true)
                .build();

        ScheduleSalary salary = ScheduleSalary.builder().build();
        ScheduleHouseProperty hp = ScheduleHouseProperty.builder().build();
        ScheduleOtherSources os = ScheduleOtherSources.builder().build();
        ScheduleExemptIncome exempt = ScheduleExemptIncome.builder().build();
        DeductionsVIA deductions = DeductionsVIA.builder().build();
        ScheduleTaxesPaid taxes = ScheduleTaxesPaid.builder().build();

        // Populate from prefill if available
        if (prefillJson != null) {
            double grossSalary = getDouble(prefillJson, "basicDetails", "salary");
            salary.setSalaryU17_1(grossSalary);

            double otherSources = getDouble(prefillJson, "basicDetails", "otherSources");
            os.setOtherIncome(otherSources);

            double sec80C = getDouble(prefillJson, "deductions", "section80C");
            deductions.setSection80C(Math.min(sec80C, 150000));

            double sec80D = getDouble(prefillJson, "deductions", "section80D");
            deductions.setSection80D(sec80D);
        }

        return Itr1FormData.builder()
                .partA(partA)
                .scheduleSalary(salary)
                .scheduleHP(hp)
                .scheduleOS(os)
                .scheduleExempt(exempt)
                .deductionsVIA(deductions)
                .taxesPaid(taxes)
                .computation(TaxComputation.builder().build())
                .validationErrors(new ArrayList<>())
                .validationWarnings(new ArrayList<>())
                .build();
    }

    private boolean isFullFormData(String json) {
        return json != null && json.contains("\"partA\"") && json.contains("\"scheduleSalary\"");
    }

    private double getDouble(JsonNode json, String parent, String field) {
        JsonNode parentNode = json.path(parent);
        if (!parentNode.isMissingNode() && parentNode.has(field)) {
            return parentNode.get(field).asDouble(0.0);
        }
        return 0.0;
    }
}
