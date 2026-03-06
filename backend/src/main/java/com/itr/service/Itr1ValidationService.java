package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr1FormData.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ITR-1 Validation Service — CBDT AY 2025-26 Validation Rules V1.1.
 * Rule references correspond to the official CBDT document.
 */
@Service
public class Itr1ValidationService {

    /**
     * Validate form data against CBDT rules. Returns list of errors.
     */
    public List<String> validate(Itr1FormData formData) {
        List<String> errors = new ArrayList<>();

        if (formData == null) {
            errors.add("Form data is null");
            return errors;
        }

        validatePartA(formData.getPartA(), errors);
        validateSalary(formData, errors);
        validateHouseProperty(formData, errors);
        validateOtherSources(formData, errors);
        validateDeductions(formData, errors);
        validateTaxComputation(formData, errors);
        validateTaxesPaid(formData, errors);

        return errors;
    }

    /**
     * Get warnings (non-blocking).
     */
    public List<String> getWarnings(Itr1FormData formData) {
        List<String> warnings = new ArrayList<>();

        if (formData == null) return warnings;

        boolean isNew = formData.getPartA() != null && formData.getPartA().isNewTaxRegime();

        // Compare regime tax and suggest
        if (formData.getComputation() != null
                && formData.getComputation().getOldRegime() != null
                && formData.getComputation().getNewRegime() != null) {
            double oldTax = formData.getComputation().getOldRegime().getTotalTax();
            double newTax = formData.getComputation().getNewRegime().getTotalTax();
            if (isNew && oldTax < newTax) {
                warnings.add("Old regime results in lower tax (₹" + fmt(oldTax)
                        + " vs ₹" + fmt(newTax) + "). Consider switching.");
            } else if (!isNew && newTax < oldTax) {
                warnings.add("New regime results in lower tax (₹" + fmt(newTax)
                        + " vs ₹" + fmt(oldTax) + "). Consider switching.");
            }
        }

        return warnings;
    }

    // ═══════════════════════════════════════════════════════════
    // Part A Validations
    // ═══════════════════════════════════════════════════════════
    private void validatePartA(PartA_GeneralInfo partA, List<String> errors) {
        if (partA == null) {
            errors.add("Part A (General Information) is required");
            return;
        }
        if (partA.getPan() == null || !partA.getPan().matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) {
            errors.add("Rule: Invalid PAN format");
        }
        if (partA.getAssesseeName() == null || partA.getAssesseeName().isBlank()) {
            errors.add("Rule: Assessee name is required");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Salary Validations [Rules 59-77, 112, 224]
    // ═══════════════════════════════════════════════════════════
    private void validateSalary(Itr1FormData formData, List<String> errors) {
        ScheduleSalary sal = formData.getScheduleSalary();
        if (sal == null) return;

        boolean isNew = formData.getPartA() != null && formData.getPartA().isNewTaxRegime();

        // Rule 59: Gross salary = sum of components
        double expectedGross = sal.getSalaryU17_1() + sal.getPerquisitesU17_2()
                + sal.getProfitsU17_3() + sal.getIncomeU89A_notified() + sal.getIncomeU89A_other();
        if (Math.abs(sal.getGrossSalary() - expectedGross) > 1) {
            errors.add("Rule 59: Gross Salary must equal sum of 17(1)+17(2)+17(3)+89A");
        }

        // Rule 63: Exempt u/s 10 cannot exceed gross salary
        if (sal.getAllowancesExemptU10() > sal.getGrossSalary()) {
            errors.add("Rule 63: Total exempt u/s 10 cannot exceed Gross Salary");
        }

        // Rule 112/224: Standard deduction cap
        double maxStdDed = isNew ? 75000 : 50000;
        if (sal.getStandardDeduction() > maxStdDed) {
            errors.add("Rule " + (isNew ? "224" : "112")
                    + ": Standard deduction cannot exceed ₹" + fmt(maxStdDed));
        }

        // Rule 164: Entertainment allowance not in new regime
        if (isNew && sal.getEntertainmentAllowance() > 0) {
            errors.add("Rule 164: Entertainment allowance deduction not allowed in new regime");
        }

        // Rule 169: Professional tax not in new regime
        if (isNew && sal.getProfessionalTax() > 0) {
            errors.add("Rule 169: Professional tax deduction not allowed in new regime");
        }

        // Rule 58: Entertainment allowance only for CG/SG/PSU
        if (!isNew && sal.getEntertainmentAllowance() > 0) {
            String nature = formData.getPartA() != null ?
                    formData.getPartA().getNatureOfEmployment() : "others";
            if (!"CG".equals(nature) && !"SG".equals(nature) && !"PSU".equals(nature)) {
                errors.add("Rule 58: Entertainment allowance deduction only for CG/SG/PSU employees");
            }
        }

        // Negative value checks
        if (sal.getSalaryU17_1() < 0) errors.add("Rule: Salary u/s 17(1) cannot be negative");
        if (sal.getPerquisitesU17_2() < 0) errors.add("Rule: Perquisites u/s 17(2) cannot be negative");
        if (sal.getProfitsU17_3() < 0) errors.add("Rule: Profits u/s 17(3) cannot be negative");
    }

    // ═══════════════════════════════════════════════════════════
    // House Property Validations [Rules 43-49, 161, 163, 263]
    // ═══════════════════════════════════════════════════════════
    private void validateHouseProperty(Itr1FormData formData, List<String> errors) {
        ScheduleHouseProperty hp = formData.getScheduleHP();
        if (hp == null) return;

        boolean isNew = formData.getPartA() != null && formData.getPartA().isNewTaxRegime();
        boolean isSelfOccupied = "self_occupied".equals(hp.getPropertyType());

        // Rule 49: Municipal tax not allowed for self-occupied
        if (isSelfOccupied && hp.getMunicipalTaxPaid() > 0) {
            errors.add("Rule 49: Municipal tax deduction not allowed for self-occupied property");
        }

        // Rule 163/263: Interest not allowed for self-occupied in new regime
        if (isNew && isSelfOccupied && hp.getInterestOnLoanU24b() > 0) {
            errors.add("Rule 163/263: Interest on self-occupied property not allowed in new regime");
        }

        // Rule 48: Self-occupied interest max 200000 in old regime
        if (!isNew && isSelfOccupied && hp.getInterestOnLoanU24b() > 200000) {
            errors.add("Rule 48: Interest on self-occupied property max ₹2,00,000 in old regime");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Other Sources Validations [Rules 50-56, 223]
    // ═══════════════════════════════════════════════════════════
    private void validateOtherSources(Itr1FormData formData, List<String> errors) {
        ScheduleOtherSources os = formData.getScheduleOS();
        if (os == null) return;

        boolean isNew = formData.getPartA() != null && formData.getPartA().isNewTaxRegime();

        // Rule 54/223: Family pension deduction cap
        if (os.getFamilyPension() > 0) {
            double maxDed = isNew ? 25000 : 15000;
            double oneThird = os.getFamilyPension() / 3.0;
            double expectedDed = Math.min(oneThird, maxDed);
            if (os.getDeductionU57iia() > expectedDed + 1) {
                errors.add("Rule " + (isNew ? "223" : "54")
                        + ": Family pension deduction exceeds allowed limit");
            }
        }

        // Rule 146: Dividend quarterly breakup must match total
        double dividendSum = os.getDividendQ1() + os.getDividendQ2()
                + os.getDividendQ3() + os.getDividendQ4();
        if (os.getDividendIncome() > 0 && Math.abs(dividendSum - os.getDividendIncome()) > 1
                && dividendSum > 0) {
            errors.add("Rule 146: Quarterly dividend breakup must match total dividend income");
        }

        // Negative checks
        if (os.getSavingsInterest() < 0) errors.add("Rule: Savings interest cannot be negative");
        if (os.getFamilyPension() < 0) errors.add("Rule: Family pension cannot be negative");
    }

    // ═══════════════════════════════════════════════════════════
    // Deductions Validations [Rules 115-176]
    // ═══════════════════════════════════════════════════════════
    private void validateDeductions(Itr1FormData formData, List<String> errors) {
        DeductionsVIA ded = formData.getDeductionsVIA();
        if (ded == null) return;

        boolean isNew = formData.getPartA() != null && formData.getPartA().isNewTaxRegime();

        if (isNew) {
            // Rules 147-176: In new regime only 80CCD(2) is allowed
            if (ded.getSection80C() > 0) errors.add("Rule 147: 80C not allowed in new regime");
            if (ded.getSection80CCC() > 0) errors.add("Rule 154: 80CCC not allowed in new regime");
            if (ded.getSection80CCD1() > 0) errors.add("Rule 155: 80CCD(1) not allowed in new regime");
            if (ded.getSection80CCD1B() > 0) errors.add("Rule 157: 80CCD(1B) not allowed in new regime");
            if (ded.getSection80D() > 0) errors.add("Rule 158: 80D not allowed in new regime");
            if (ded.getSection80DD() > 0) errors.add("Rule 159: 80DD not allowed in new regime");
            if (ded.getSection80DDB() > 0) errors.add("Rule 160: 80DDB not allowed in new regime");
            if (ded.getSection80E() > 0) errors.add("Rule: 80E not allowed in new regime");
            if (ded.getSection80EE() > 0) errors.add("Rule 170: 80EE not allowed in new regime");
            if (ded.getSection80EEA() > 0) errors.add("Rule 171: 80EEA not allowed in new regime");
            if (ded.getSection80EEB() > 0) errors.add("Rule 172: 80EEB not allowed in new regime");
            if (ded.getSection80G() > 0) errors.add("Rule 173: 80G not allowed in new regime");
            if (ded.getSection80GG() > 0) errors.add("Rule 174: 80GG not allowed in new regime");
            if (ded.getSection80GGA() > 0) errors.add("Rule: 80GGA not allowed in new regime");
            if (ded.getSection80GGC() > 0) errors.add("Rule: 80GGC not allowed in new regime");
            if (ded.getSection80TTA() > 0) errors.add("Rule 175: 80TTA not allowed in new regime");
            if (ded.getSection80TTB() > 0) errors.add("Rule 176: 80TTB not allowed in new regime");
            if (ded.getSection80U() > 0) errors.add("Rule: 80U not allowed in new regime");
        } else {
            // Old regime caps
            double combined = ded.getSection80C() + ded.getSection80CCC() + ded.getSection80CCD1();
            if (combined > 150000) {
                errors.add("Rule: 80C+80CCC+80CCD(1) combined cannot exceed ₹1,50,000");
            }
            if (ded.getSection80CCD1B() > 50000) {
                errors.add("Rule 115: 80CCD(1B) cannot exceed ₹50,000");
            }
            if (ded.getSection80EE() > 50000) {
                errors.add("Rule 122: 80EE cannot exceed ₹50,000");
            }
            if (ded.getSection80EEA() > 150000) {
                errors.add("Rule 123: 80EEA cannot exceed ₹1,50,000");
            }
            // Rule 124: Only one of 80EE/80EEA
            if (ded.getSection80EE() > 0 && ded.getSection80EEA() > 0) {
                errors.add("Rule 124: 80EE and 80EEA are mutually exclusive");
            }
            if (ded.getSection80EEB() > 150000) {
                errors.add("Rule 125: 80EEB cannot exceed ₹1,50,000");
            }
            if (ded.getSection80GG() > 60000) {
                errors.add("Rule 114: 80GG cannot exceed ₹60,000");
            }
            if (ded.getSection80TTA() > 10000) {
                errors.add("Rule: 80TTA cannot exceed ₹10,000");
            }
            if (ded.getSection80TTB() > 50000) {
                errors.add("Rule: 80TTB cannot exceed ₹50,000");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Tax Computation Validations [Rules 117, 200-201]
    // ═══════════════════════════════════════════════════════════
    private void validateTaxComputation(Itr1FormData formData, List<String> errors) {
        TaxComputation comp = formData.getComputation();
        if (comp == null) return;

        // Rule 117: ITR-1 income cap 50 lakhs
        if (comp.getTotalTaxableIncome() > 5000000) {
            errors.add("Rule 117: Total income exceeds ₹50 lakhs — ITR-1 not applicable, use ITR-2");
        }

        // Rule 141: Total tax liability cannot be negative
        if (comp.getTotalTaxLiability() < 0) {
            errors.add("Rule 141: Total tax liability cannot be negative");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Taxes Paid Validations [Rules 95-111, 193]
    // ═══════════════════════════════════════════════════════════
    private void validateTaxesPaid(Itr1FormData formData, List<String> errors) {
        ScheduleTaxesPaid tp = formData.getTaxesPaid();
        if (tp == null) return;

        // Rule 193: TDS on salary cannot exceed gross salary
        if (formData.getScheduleSalary() != null) {
            if (tp.getTdsOnSalary() > formData.getScheduleSalary().getGrossSalary()) {
                errors.add("Rule 193: TDS on salary cannot exceed Gross Salary");
            }
        }

        // Negative checks
        if (tp.getTdsOnSalary() < 0) errors.add("Rule: TDS on salary cannot be negative");
        if (tp.getTdsOtherThanSalary() < 0) errors.add("Rule: TDS (other) cannot be negative");
        if (tp.getTcs() < 0) errors.add("Rule: TCS cannot be negative");
        if (tp.getAdvanceTax() < 0) errors.add("Rule: Advance tax cannot be negative");
        if (tp.getSelfAssessmentTax() < 0) errors.add("Rule: Self-assessment tax cannot be negative");
    }

    private String fmt(double amount) {
        if (amount == (long) amount) {
            return String.format("%,d", (long) amount);
        }
        return String.format("%,.2f", amount);
    }
}
