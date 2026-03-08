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
import com.itr.mapper.ITR1JsonBuilder;
import com.itr.mapper.PrefillMapper;
import com.itr.model.ITR1Result;
import com.itr.model.PreFillData;
import com.itr.model.TaxRegime;
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
    private final ITR1ValidationService validationService;
    private final ObjectMapper objectMapper;
    private final PrefillService prefillService;
    private final PrefillMapper prefillMapper;
    private final ITR1JsonBuilder itr1JsonBuilder;

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

        // Full recompute so ALL derived fields stay in sync with inputs
        recomputeAll(formData);

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

        recomputeAll(formData);

        // ───── Validate ─────
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        formData.setValidationErrors(errors);
        formData.setValidationWarnings(warnings);

        // ───── Save ─────
        try {
            yearData.setComputedItr1Json(objectMapper.writeValueAsString(formData));
            yearData.setStatus(errors.isEmpty() ? "computed" : "draft");
            yearDataRepository.save(yearData);
        } catch (JsonProcessingException e) {
            throw new BusinessLogicException("Failed to save computed form data");
        }

        return formData;
    }

    /**
     * Full recomputation of all derived fields from user inputs.
     */
    private void recomputeAll(Itr1FormData formData) {
        boolean isNewRegime = formData.getPartA() != null && formData.getPartA().isNewTaxRegime();
        String natureOfEmployment = formData.getPartA() != null ?
                formData.getPartA().getNatureOfEmployment() : "others";

        // 1. Compute Schedule Salary
        computeSalary(formData, isNewRegime, natureOfEmployment);
        // 2. Compute Schedule HP
        computeHouseProperty(formData, isNewRegime);
        // 3. Compute Schedule OS
        computeOtherSources(formData, isNewRegime);
        // 4. Compute Exempt Income
        computeExemptIncome(formData);

        // 5. Compute GTI
        double gti = (formData.getScheduleSalary() != null ? formData.getScheduleSalary().getIncomeFromSalary() : 0)
                + (formData.getScheduleHP() != null ? formData.getScheduleHP().getIncomeFromHP() : 0)
                + (formData.getScheduleOS() != null ? formData.getScheduleOS().getIncomeFromOtherSources() : 0);

        // 6. Compute Deductions VI-A
        computeDeductions(formData, isNewRegime, gti, natureOfEmployment);

        double totalDeductions = formData.getDeductionsVIA() != null ?
                formData.getDeductionsVIA().getTotalDeductions() : 0;

        // 7. Tax Computation
        TaxComputation computation = taxService.computeTax(gti, totalDeductions, isNewRegime);

        // 8. Taxes Paid
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
        List<String> errors = new java.util.ArrayList<>();
        if (!errors.isEmpty()) {
            throw new BusinessLogicException("Form has validation errors: " + String.join("; ", errors));
        }

        // Convert form data to e-filing schema models
        PreFillData prefill = convertToPreFillData(formData, client, yearData);
        ITR1Result result = convertToITR1Result(formData);

        yearData.setStatus("filed");
        yearDataRepository.save(yearData);

        return itr1JsonBuilder.build(prefill, result);
    }

    // ─────────────────────────────────────────────────────────
    // Convert Itr1FormData → PreFillData (personal/address/TDS)
    // ─────────────────────────────────────────────────────────
    private PreFillData convertToPreFillData(Itr1FormData formData, Client client, ClientYearData yearData) {
        PreFillData prefill = new PreFillData();
        PartA_GeneralInfo partA = formData.getPartA();

        // Try to parse stored raw prefill for TDS entries and address
        PreFillData rawPrefill = null;
        if (yearData.getRawPrefillJson() != null) {
            try {
                rawPrefill = prefillMapper.map(yearData.getRawPrefillJson());
            } catch (Exception e) {
                log.warn("Could not parse raw prefill JSON for TDS data", e);
            }
        }

        // Personal info
        if (partA != null) {
            String fullName = partA.getAssesseeName() != null ? partA.getAssesseeName() : "";
            String[] nameParts = fullName.trim().split("\\s+", 2);
            prefill.firstName = nameParts.length > 0 ? nameParts[0] : "";
            prefill.lastName = nameParts.length > 1 ? nameParts[1] : "";
            prefill.pan = partA.getPan();
            prefill.aadhaarCardNo = partA.getAadhaar();
            prefill.dob = partA.getDob();
            prefill.email = partA.getEmail();
            prefill.mobile = partA.getMobile();
            prefill.employerName = partA.getEmployerName();
            prefill.employerTAN = partA.getEmployerTAN();
            prefill.optingNewTaxRegime = partA.isNewTaxRegime() ? 2 : 1;
            prefill.residentialStatus = "RES";

            String nature = partA.getNatureOfEmployment();
            if ("CG".equals(nature)) prefill.employerCategoryCode = "GOV";
            else if ("SG".equals(nature)) prefill.employerCategoryCode = "GOV";
            else if ("PSU".equals(nature)) prefill.employerCategoryCode = "GOV";
            else prefill.employerCategoryCode = "OTH";
        }

        // Address from raw prefill or defaults
        if (rawPrefill != null) {
            prefill.flatNo = rawPrefill.flatNo;
            prefill.premisesName = rawPrefill.premisesName;
            prefill.road = rawPrefill.road;
            prefill.area = rawPrefill.area;
            prefill.city = rawPrefill.city;
            prefill.state = rawPrefill.state;
            prefill.pinCode = rawPrefill.pinCode;
            prefill.country = rawPrefill.country;
        }

        // Salary prefill fields
        ScheduleSalary sal = formData.getScheduleSalary();
        if (sal != null) {
            prefill.salary17_1 = Math.round(sal.getSalaryU17_1());
            prefill.perquisites17_2 = Math.round(sal.getPerquisitesU17_2());
            prefill.profitsInLieu17_3 = Math.round(sal.getProfitsU17_3());
            prefill.professionalTaxUs16iii = Math.round(sal.getProfessionalTax());
            prefill.totalAllwncExemptUs10 = Math.round(sal.getAllowancesExemptU10());
        }

        // House Property
        ScheduleHouseProperty hp = formData.getScheduleHP();
        if (hp != null) {
            String pt = hp.getPropertyType();
            if ("self_occupied".equals(pt)) prefill.typeOfHP = "S";
            else if ("let_out".equals(pt)) prefill.typeOfHP = "L";
            else if ("deemed_let_out".equals(pt)) prefill.typeOfHP = "D";
            else prefill.typeOfHP = "S";
            prefill.grossRentReceived = Math.round(hp.getGrossRent());
            prefill.taxPaidLocalAuth = Math.round(hp.getMunicipalTaxPaid());
            prefill.interestOnBorrowedCapital = Math.round(hp.getInterestOnLoanU24b());
            prefill.arrearsUnrealizedRentRcvd = Math.round(hp.getArrearsUnrealizedRent());
        }

        // Other Sources
        ScheduleOtherSources os = formData.getScheduleOS();
        if (os != null) {
            prefill.savingsAccountInterest = Math.round(os.getSavingsInterest());
            prefill.fdInterest = Math.round(os.getDepositInterest());
            prefill.dividendIncome = Math.round(os.getDividendIncome());
            prefill.familyPension = Math.round(os.getFamilyPension());
            prefill.interestFromITRefund = Math.round(os.getIncomeTaxRefundInterest());
            prefill.otherIncome = Math.round(os.getOtherIncome());
        }

        // Deductions
        DeductionsVIA ded = formData.getDeductionsVIA();
        if (ded != null) {
            prefill.section80C = Math.round(ded.getSection80C());
            prefill.section80CCC = Math.round(ded.getSection80CCC());
            prefill.section80CCD_Employee = Math.round(ded.getSection80CCD1());
            prefill.section80CCD_1B = Math.round(ded.getSection80CCD1B());
            prefill.section80CCD_Employer = Math.round(ded.getSection80CCD2());
            prefill.section80D = Math.round(ded.getSection80D());
            prefill.section80DD = Math.round(ded.getSection80DD());
            prefill.section80DDB = Math.round(ded.getSection80DDB());
            prefill.section80E = Math.round(ded.getSection80E());
            prefill.section80EE = Math.round(ded.getSection80EE());
            prefill.section80EEA = Math.round(ded.getSection80EEA());
            prefill.section80EEB = Math.round(ded.getSection80EEB());
            prefill.section80G = Math.round(ded.getSection80G());
            prefill.section80GG = Math.round(ded.getSection80GG());
            prefill.section80GGA = Math.round(ded.getSection80GGA());
            prefill.section80GGC = Math.round(ded.getSection80GGC());
            prefill.section80TTA = Math.round(ded.getSection80TTA());
            prefill.section80TTB = Math.round(ded.getSection80TTB());
            prefill.section80U = Math.round(ded.getSection80U());
            prefill.section80CCH = Math.round(ded.getSection80CCH());
        }

        // Relief
        TaxComputation comp = formData.getComputation();
        if (comp != null) {
            prefill.reliefUs89 = Math.round(comp.getReliefU89());
        }

        // TDS entries from raw prefill (these have TAN/deductor details)
        if (rawPrefill != null) {
            prefill.tdsOnSalary = rawPrefill.tdsOnSalary;
            prefill.tdsOnOtherIncome = rawPrefill.tdsOnOtherIncome;
            prefill.tcsEntries = rawPrefill.tcsEntries;
            prefill.taxPayments = rawPrefill.taxPayments;
        }

        return prefill;
    }

    // ─────────────────────────────────────────────────────────
    // Convert Itr1FormData → ITR1Result (computed values)
    // ─────────────────────────────────────────────────────────
    private ITR1Result convertToITR1Result(Itr1FormData formData) {
        ITR1Result r = new ITR1Result();

        // Salary
        ScheduleSalary sal = formData.getScheduleSalary();
        if (sal != null) {
            r.salary17_1 = Math.round(sal.getSalaryU17_1());
            r.perquisites17_2 = Math.round(sal.getPerquisitesU17_2());
            r.profitsInLieu17_3 = Math.round(sal.getProfitsU17_3());
            r.grossSalary = Math.round(sal.getGrossSalary());
            r.allowancesExemptUs10 = Math.round(sal.getAllowancesExemptU10());
            r.netSalary = Math.round(sal.getNetSalary());
            r.stdDeductionUs16ia = Math.round(sal.getStandardDeduction());
            r.entertainmentAlw16ii = Math.round(sal.getEntertainmentAllowance());
            r.professionalTaxUs16iii = Math.round(sal.getProfessionalTax());
            r.deductionUs16 = r.stdDeductionUs16ia + r.entertainmentAlw16ii + r.professionalTaxUs16iii;
            r.incomeFromSalary = Math.round(sal.getIncomeFromSalary());
        }

        // House Property
        ScheduleHouseProperty hp = formData.getScheduleHP();
        if (hp != null) {
            String pt = hp.getPropertyType();
            if ("self_occupied".equals(pt)) r.typeOfHP = "S";
            else if ("let_out".equals(pt)) r.typeOfHP = "L";
            else if ("deemed_let_out".equals(pt)) r.typeOfHP = "D";
            else r.typeOfHP = "S";
            r.grossRentReceived = Math.round(hp.getGrossRent());
            r.taxPaidLocalAuth = Math.round(hp.getMunicipalTaxPaid());
            r.annualValue = Math.round(hp.getAnnualValue());
            r.standardDeductionHP = Math.round(hp.getStandardDeduction30Pct());
            r.interestPayable = Math.round(hp.getInterestOnLoanU24b());
            r.arrearsUnrealizedRent = Math.round(hp.getArrearsUnrealizedRent());
            r.totalIncomeOfHP = Math.round(hp.getIncomeFromHP());
        }

        // Other Sources
        ScheduleOtherSources os = formData.getScheduleOS();
        if (os != null) {
            r.savingsAccountInterest = Math.round(os.getSavingsInterest());
            r.fdInterest = Math.round(os.getDepositInterest());
            r.dividendIncome = Math.round(os.getDividendIncome());
            r.familyPension = Math.round(os.getFamilyPension());
            r.otherIncome = Math.round(os.getOtherIncome());
            r.incomeOthSrc = Math.round(os.getGrossOtherSources());
            r.deductionUs57iia = Math.round(os.getDeductionU57iia());
            r.netIncomeOthSrc = Math.round(os.getIncomeFromOtherSources());
        }

        // Deductions
        DeductionsVIA ded = formData.getDeductionsVIA();
        if (ded != null) {
            r.sec80C = Math.round(ded.getSection80C());
            r.sec80CCC = Math.round(ded.getSection80CCC());
            r.sec80CCD_1 = Math.round(ded.getSection80CCD1());
            r.sec80CCD_1B = Math.round(ded.getSection80CCD1B());
            r.sec80CCD_2 = Math.round(ded.getSection80CCD2());
            r.sec80D = Math.round(ded.getSection80D());
            r.sec80DD = Math.round(ded.getSection80DD());
            r.sec80DDB = Math.round(ded.getSection80DDB());
            r.sec80E = Math.round(ded.getSection80E());
            r.sec80EE = Math.round(ded.getSection80EE());
            r.sec80EEA = Math.round(ded.getSection80EEA());
            r.sec80EEB = Math.round(ded.getSection80EEB());
            r.sec80G = Math.round(ded.getSection80G());
            r.sec80GG = Math.round(ded.getSection80GG());
            r.sec80GGA = Math.round(ded.getSection80GGA());
            r.sec80GGC = Math.round(ded.getSection80GGC());
            r.sec80TTA = Math.round(ded.getSection80TTA());
            r.sec80TTB = Math.round(ded.getSection80TTB());
            r.sec80U = Math.round(ded.getSection80U());
            r.sec80CCH = Math.round(ded.getSection80CCH());
            r.totalChapVIADeductions = Math.round(ded.getTotalDeductions());
        }

        // Tax Computation
        TaxComputation comp = formData.getComputation();
        if (comp != null) {
            r.grossTotalIncome = Math.round(comp.getGrossTotalIncome());
            r.totalIncome = Math.round(comp.getTotalTaxableIncome());
            r.totalTaxPayable = Math.round(comp.getTaxOnIncome());
            r.rebate87A = Math.round(comp.getRebateU87A());
            r.taxAfterRebate = Math.round(comp.getTaxAfterRebate());
            r.educationCess = Math.round(comp.getCessAt4Pct());
            r.grossTaxLiability = Math.round(comp.getTotalTaxLiability());
            r.reliefUs89 = Math.round(comp.getReliefU89());
            r.netTaxLiability = Math.round(comp.getTotalTaxAfterRelief());
            r.totalTaxFeeInterest = Math.round(comp.getTotalTaxAndInterest());
            r.intrstPayUs234A = 0;
            r.intrstPayUs234B = 0;
            r.intrstPayUs234C = 0;
            r.lateFilingFee234F = 0;
            r.totalInterestFee = Math.round(comp.getInterestPayable());
            r.refundDue = Math.round(comp.getRefundDue());
            r.balTaxPayable = Math.round(comp.getBalanceTaxPayable());

            // Tax regime
            r.taxRegime = comp.getSelectedRegime() != null && comp.getSelectedRegime().equals("old")
                    ? TaxRegime.OLD_REGIME : TaxRegime.NEW_REGIME;
        }

        // Taxes Paid
        ScheduleTaxesPaid tp = formData.getTaxesPaid();
        if (tp != null) {
            r.tdsSalaryTotal = Math.round(tp.getTdsOnSalary());
            r.tdsOtherTotal = Math.round(tp.getTdsOtherThanSalary() + tp.getTds3());
            r.tdsTotal = r.tdsSalaryTotal + r.tdsOtherTotal;
            r.tcsTotal = Math.round(tp.getTcs());
            r.advanceTaxTotal = Math.round(tp.getAdvanceTax());
            r.selfAssessmentTaxTotal = Math.round(tp.getSelfAssessmentTax());
            r.totalTaxesPaid = Math.round(tp.getTotalTaxesPaid());
        }

        return r;
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
        PreFillData prefillData = null;
        
        // Use PrefillMapper to extract data from raw JSON
        if (yearData.getRawPrefillJson() != null) {
            try {
                prefillData = prefillMapper.map(yearData.getRawPrefillJson());
            } catch (Exception e) {
                log.warn("Failed to parse prefill JSON with mapper", e);
            }
        }

        // Part A - General Info
        PartA_GeneralInfo partA = PartA_GeneralInfo.builder()
                .assesseeName(client.getName())
                .pan(client.getPan())
                .aadhaar(client.getAadhaar() != null ? client.getAadhaar() : "")
                .dob(client.getDob() != null ? client.getDob().toString() : "")
                .email(client.getEmail() != null ? client.getEmail() : "")
                .mobile(client.getMobile() != null ? client.getMobile() : "")
                .assessmentYear(yearData.getAssessmentYear())
                .newTaxRegime(prefillData != null && prefillData.optingNewTaxRegime == 2)
                .residentialStatus(prefillData != null ? prefillData.residentialStatus : "RES")
                .natureOfEmployment(prefillData != null ? prefillData.employerCategoryCode : "OTH")
                .employerName(prefillData != null && prefillData.employerName != null ? prefillData.employerName : "")
                .employerTAN(prefillData != null && prefillData.employerTAN != null ? prefillData.employerTAN : "")
                .build();

        // Schedule Salary
        ScheduleSalary salary = ScheduleSalary.builder()
                .salaryU17_1(prefillData != null ? prefillData.salary17_1 : 0)
                .perquisitesU17_2(prefillData != null ? prefillData.perquisites17_2 : 0)
                .profitsU17_3(prefillData != null ? prefillData.profitsInLieu17_3 : 0)
                .allowancesExemptU10(prefillData != null ? prefillData.totalAllwncExemptUs10 : 0)
                .professionalTax(prefillData != null ? prefillData.professionalTaxUs16iii : 0)
                .build();

        // Schedule House Property
        ScheduleHouseProperty hp = ScheduleHouseProperty.builder()
                .propertyType(prefillData != null ? prefillData.typeOfHP : "")
                .grossRent(prefillData != null ? prefillData.grossRentReceived : 0)
                .municipalTaxPaid(prefillData != null ? prefillData.taxPaidLocalAuth : 0)
                .interestOnLoanU24b(prefillData != null ? prefillData.interestOnBorrowedCapital : 0)
                .build();

        // Schedule Other Sources
        ScheduleOtherSources os = ScheduleOtherSources.builder()
                .savingsInterest(prefillData != null ? prefillData.savingsAccountInterest : 0)
                .depositInterest(prefillData != null ? prefillData.fdInterest : 0)
                .dividendIncome(prefillData != null ? prefillData.dividendIncome : 0)
                .familyPension(prefillData != null ? prefillData.familyPension : 0)
                .otherIncome(prefillData != null ? prefillData.otherIncome : 0)
                .build();

        // Deductions VI-A
        DeductionsVIA deductions = DeductionsVIA.builder()
                .section80C(prefillData != null ? prefillData.section80C : 0)
                .section80CCC(prefillData != null ? prefillData.section80CCC : 0)
                .section80CCD1(prefillData != null ? prefillData.section80CCD_Employee : 0)
                .section80CCD1B(prefillData != null ? prefillData.section80CCD_1B : 0)
                .section80CCD2(prefillData != null ? prefillData.section80CCD_Employer : 0)
                .section80D(prefillData != null ? prefillData.section80D : 0)
                .section80DD(prefillData != null ? prefillData.section80DD : 0)
                .section80DDB(prefillData != null ? prefillData.section80DDB : 0)
                .section80E(prefillData != null ? prefillData.section80E : 0)
                .section80EE(prefillData != null ? prefillData.section80EE : 0)
                .section80EEA(prefillData != null ? prefillData.section80EEA : 0)
                .section80EEB(prefillData != null ? prefillData.section80EEB : 0)
                .section80G(prefillData != null ? prefillData.section80G : 0)
                .section80GG(prefillData != null ? prefillData.section80GG : 0)
                .section80GGA(prefillData != null ? prefillData.section80GGA : 0)
                .section80GGC(prefillData != null ? prefillData.section80GGC : 0)
                .section80TTA(prefillData != null ? prefillData.section80TTA : 0)
                .section80TTB(prefillData != null ? prefillData.section80TTB : 0)
                .section80U(prefillData != null ? prefillData.section80U : 0)
                .build();

        // Taxes Paid
        ScheduleTaxesPaid taxes = ScheduleTaxesPaid.builder()
                .tdsOnSalary(prefillData != null && prefillData.tdsOnSalary != null ? 
                    prefillData.tdsOnSalary.stream().mapToLong(t -> t.taxClaimed).sum() : 0)
                .tdsOtherThanSalary(prefillData != null && prefillData.tdsOnOtherIncome != null ?
                    prefillData.tdsOnOtherIncome.stream().mapToLong(t -> t.taxClaimed).sum() : 0)
                .tcs(prefillData != null && prefillData.tcsEntries != null ?
                    prefillData.tcsEntries.stream().mapToLong(t -> t.taxClaimed).sum() : 0)
                .advanceTax(prefillData != null && prefillData.taxPayments != null ?
                    prefillData.taxPayments.stream().mapToLong(t -> t.amount).sum() : 0)
                .build();

        return Itr1FormData.builder()
                .partA(partA)
                .scheduleSalary(salary)
                .scheduleHP(hp)
                .scheduleOS(os)
                .scheduleExempt(ScheduleExemptIncome.builder().build())
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
