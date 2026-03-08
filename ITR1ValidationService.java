package com.itr1.calculator.service;

import com.itr1.calculator.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates computed ITR1Result against CBDT Validation Rules AY 2025-26 V1.1.
 *
 * Rules are tagged with their rule number from the official document.
 * Category A = return blocked; Category B = defect notice; Category D = claim may not be allowed.
 */
@Service
public class ITR1ValidationService {

    public void validate(PreFillData prefill, ITR1Result result, int ageYears) {
        result.errors   = new ArrayList<>();
        result.warnings = new ArrayList<>();
        result.infos    = new ArrayList<>();

        boolean isOldRegime = result.taxRegime == TaxRegime.OLD_REGIME;

        // ── Category A Rules ──────────────────────────────────────────────────

        // Rule 1: 80C + 80CCC + 80CCD(1) ≤ 1,50,000 (old regime)
        if (isOldRegime) {
            long total80CGroup = result.sec80C + result.sec80CCC + result.sec80CCD_1;
            if (total80CGroup > 150_000) {
                addError(result, 1, "Section80C/CCC/CCD1",
                    "Sum of deductions u/s 80C, 80CCC & 80CCD(1) cannot exceed ₹1,50,000. " +
                    "Current: ₹" + total80CGroup);
            }
        }

        // Rule 2 & 3: 80CCD(1) limit — 20% of GTI for pensioners, 10% of salary for employees
        if (isOldRegime && result.sec80CCD_1 > 0) {
            EmployerCategory ec = EmployerCategory.fromCode(prefill.employerCategoryCode);
            if (ec.isPensioner()) {
                long maxAllowed = result.grossTotalIncome * 20 / 100;
                if (result.sec80CCD_1 > maxAllowed) {
                    addError(result, 2, "Section80CCD1",
                        "Deduction u/s 80CCD(1) for pensioner cannot exceed 20% of Gross Total Income (₹" + maxAllowed + ").");
                }
            } else {
                long maxAllowed = result.salary17_1 * 10 / 100;
                if (result.sec80CCD_1 > maxAllowed) {
                    addError(result, 3, "Section80CCD1",
                        "Deduction u/s 80CCD(1) for employee cannot exceed 10% of salary (₹" + maxAllowed + ").");
                }
            }
        }

        // Rule 4: 80CCD(2) ≤ 10% of salary (employer other than CG/SG), 14% for CG/SG
        if (isOldRegime && result.sec80CCD_2 > 0) {
            EmployerCategory ec = EmployerCategory.fromCode(prefill.employerCategoryCode);
            if (ec.isCentralOrStateGovt()) {
                // Rule 121: 14% for CG/SG
                long maxAllowed = result.salary17_1 * 14 / 100;
                if (result.sec80CCD_2 > maxAllowed) {
                    addError(result, 121, "Section80CCD2",
                        "Deduction u/s 80CCD(2) for CG/SG employer cannot exceed 14% of salary (₹" + maxAllowed + ").");
                }
            } else {
                long maxAllowed = result.salary17_1 * 10 / 100;
                if (result.sec80CCD_2 > maxAllowed) {
                    addError(result, 4, "Section80CCD2",
                        "Deduction u/s 80CCD(2) cannot exceed 10% of salary (₹" + maxAllowed + ").");
                }
            }
        }

        // Rule 116: 80CCD(2) cannot be claimed by pensioners / not-applicable
        if (result.sec80CCD_2 > 0) {
            EmployerCategory ec = EmployerCategory.fromCode(prefill.employerCategoryCode);
            if (ec.isPensioner()) {
                addError(result, 116, "Section80CCD2",
                    "Deduction u/s 80CCD(2) cannot be claimed by pensioners/not-applicable.");
            }
        }

        // Rule 5 & 7: 80DDB limits (old regime)
        if (isOldRegime && result.sec80DDB > 0) {
            if (result.sec80DDB > 100_000) {
                addError(result, 5, "Section80DDB",
                    "Deduction u/s 80DDB cannot exceed ₹1,00,000.");
            }
            if (prefill.section80DDB_category == null || prefill.section80DDB_category.isEmpty()) {
                addError(result, 6, "Section80DDB",
                    "Category description (self/dependent or senior citizen) must be provided for 80DDB.");
            }
            if ("1".equals(prefill.section80DDB_category) && result.sec80DDB > 40_000) {
                addError(result, 7, "Section80DDB",
                    "Maximum deduction u/s 80DDB for Self/Dependent is ₹40,000.");
            }
        }

        // Rule 8: 80G claimed but Schedule 80G not filled
        if (isOldRegime && result.sec80G > 0 && prefill.section80G == 0) {
            addError(result, 8, "Section80G",
                "Deduction u/s 80G claimed but details not provided in Schedule 80G.");
        }

        // Rule 10: 80G in VIA > eligible amount from Schedule 80G
        if (isOldRegime && result.sec80G > prefill.section80G) {
            addError(result, 10, "Section80G",
                "Deduction u/s 80G (₹" + result.sec80G + ") exceeds eligible donation amount (₹" + prefill.section80G + ").");
        }

        // Rule 11 & 12: 80TTA max ₹10,000; restricted to savings interest
        if (isOldRegime && result.sec80TTA > 0) {
            if (result.sec80TTA > 10_000) {
                addError(result, 11, "Section80TTA",
                    "Deduction u/s 80TTA cannot exceed ₹10,000. Current: ₹" + result.sec80TTA);
            }
            if (result.sec80TTA > result.savingsAccountInterest) {
                addError(result, 12, "Section80TTA",
                    "Deduction u/s 80TTA is restricted to savings account interest income (₹" + result.savingsAccountInterest + ").");
            }
        }

        // Rule 13: 80TTA cannot be claimed by senior citizen (DOB on or before 02.04.1964)
        if (result.sec80TTA > 0 && ageYears >= 60) {
            addError(result, 13, "Section80TTA",
                "Senior citizens cannot claim deduction u/s 80TTA. Claim 80TTB instead.");
        }

        // Rule 14 & 15: 80TTB max ₹50,000; only for senior citizens
        if (result.sec80TTB > 0) {
            if (ageYears < 60) {
                addError(result, 15, "Section80TTB",
                    "Deduction u/s 80TTB is available only to Senior Citizens (age ≥ 60 years).");
            } else if (result.sec80TTB > 50_000) {
                addError(result, 14, "Section80TTB",
                    "Deduction u/s 80TTB cannot exceed ₹50,000. Current: ₹" + result.sec80TTB);
            }
        }

        // Rule 17 & 18: Total Chapter VI-A ≤ GTI; sum must match
        if (result.totalChapVIADeductions > result.grossTotalIncome) {
            addError(result, 18, "TotalChapVIADeductions",
                "Total Chapter VI-A deductions (₹" + result.totalChapVIADeductions +
                ") cannot exceed Gross Total Income (₹" + result.grossTotalIncome + ").");
        }

        // Rule 22: Old regime — GTI = Salary + HP + Other Sources + LTCG 112A
        if (isOldRegime) {
            long computedGTI = result.incomeFromSalary + result.totalIncomeOfHP + result.netIncomeOthSrc;
            if (computedGTI != result.grossTotalIncome) {
                addError(result, 22, "GrossTotalIncome",
                    "Gross Total Income (₹" + result.grossTotalIncome + ") must equal sum of income from all heads (₹" + computedGTI + ").");
            }
        }

        // Rule 23: Old regime — 87A rebate not allowed if Total Income > ₹5,00,000
        if (isOldRegime && result.rebate87A > 0 && result.totalIncome > 500_000) {
            addError(result, 23, "Rebate87A",
                "Rebate u/s 87A not allowed when Total Income exceeds ₹5,00,000 under old regime.");
        }

        // Rule 24: TotalIncome = GTI - TotalDeductions (or 0 if negative)
        long expectedTotalIncome = Math.max(0, result.grossTotalIncome - result.totalChapVIADeductions);
        if (result.totalIncome != expectedTotalIncome) {
            addError(result, 24, "TotalIncome",
                "Total Income (₹" + result.totalIncome + ") should be ₹" + expectedTotalIncome + " (GTI - Deductions).");
        }

        // Rule 25: Tax after Rebate = Tax on Total Income - Rebate u/s 87A
        long expectedTaxAfterRebate = Math.max(0, result.totalTaxPayable - result.rebate87A);
        if (result.taxAfterRebate != expectedTaxAfterRebate) {
            addError(result, 25, "TaxAfterRebate",
                "Tax after Rebate should be ₹" + expectedTaxAfterRebate + " (Tax - Rebate 87A).");
        }

        // Rule 26: Total Tax & Cess = Tax after Rebate + Education Cess
        long expectedGTL = result.taxAfterRebate + result.educationCess;
        if (result.grossTaxLiability != expectedGTL) {
            addError(result, 26, "GrossTaxLiability",
                "Total Tax & Cess (₹" + result.grossTaxLiability + ") should be ₹" + expectedGTL + ".");
        }

        // Rule 27: Total Tax, Fees & Interest = Total Tax & Cess + 234A + 234B + 234C + 234F - 89 relief
        long expectedTaxFeeInterest = result.grossTaxLiability
                + result.intrstPayUs234A + result.intrstPayUs234B
                + result.intrstPayUs234C + result.lateFilingFee234F
                - result.reliefUs89;
        if (expectedTaxFeeInterest < 0) expectedTaxFeeInterest = 0;
        if (result.totalTaxFeeInterest != expectedTaxFeeInterest) {
            addError(result, 27, "TotalTaxFeeInterest",
                "Total Tax, Fees & Interest should be ₹" + expectedTaxFeeInterest + ".");
        }

        // Rule 29: Agriculture income ≤ ₹5,000
        if (prefill.agricultureIncome > 5_000) {
            addError(result, 29, "AgricultureIncome",
                "Agriculture income shown as exempt cannot exceed ₹5,000 in ITR-1. Actual: ₹" + prefill.agricultureIncome);
        }

        // Rule 43: HP standard deduction = 30% of Annual Value
        if (result.totalIncomeOfHP != 0) {
            long expectedStdDed = result.annualValue * 30 / 100;
            if (result.standardDeductionHP != expectedStdDed) {
                addError(result, 43, "StandardDeductionHP",
                    "Standard deduction on house property must be 30% of Annual Value (₹" + expectedStdDed + ").");
            }
        }

        // Rule 46: Annual Value = Gross Rent - Tax Paid to Local Authority
        long expectedAV = result.grossRentReceived - result.taxPaidLocalAuth;
        if (result.annualValue != expectedAV && result.grossRentReceived > 0) {
            addError(result, 46, "AnnualValue",
                "Annual Value (₹" + result.annualValue + ") should equal Gross Rent - Municipal Tax (₹" + expectedAV + ").");
        }

        // Rule 47: Income chargeable under HP = AV - StdDed - Interest + Arrears
        long expectedHP = result.annualValue - result.standardDeductionHP
                - result.interestPayable + result.arrearsUnrealizedRent;
        if (result.totalIncomeOfHP != expectedHP) {
            addError(result, 47, "TotalIncomeOfHP",
                "Income under House Property should be ₹" + expectedHP + ".");
        }

        // Rule 48: Self-occupied HP — interest ≤ ₹2,00,000 (old regime)
        if (isOldRegime && "S".equalsIgnoreCase(result.typeOfHP) && result.interestPayable > 200_000) {
            addError(result, 48, "InterestPayable",
                "Interest on borrowed capital for Self-Occupied property cannot exceed ₹2,00,000.");
        }

        // Rule 49: Municipal tax not allowed for Self-Occupied
        if ("S".equalsIgnoreCase(result.typeOfHP) && result.taxPaidLocalAuth > 0) {
            addError(result, 49, "TaxPaidLocalAuth",
                "Tax paid to local authorities is not allowed for Self-Occupied house property.");
        }

        // Rule 52: Other Sources income = sum of individual entries
        long computedOtherSrc = result.savingsAccountInterest + result.fdInterest
                + result.dividendIncome + result.familyPension + result.otherIncome;
        if (result.incomeOthSrc != computedOtherSrc) {
            addError(result, 52, "IncomeOthSrc",
                "Income from Other Sources (₹" + result.incomeOthSrc + ") should equal sum of all other source items (₹" + computedOtherSrc + ").");
        }

        // Rule 53 & 54: 57(iia) deduction only for family pension, old regime; max 1/3 or ₹15,000
        if (result.deductionUs57iia > 0) {
            if (!isOldRegime) {
                addError(result, 53, "DeductionUs57iia",
                    "Deduction u/s 57(iia) for family pension is allowed only under Old Tax Regime.");
            }
            long maxDed57 = Math.min(result.familyPension / 3, 15_000);
            if (result.deductionUs57iia > maxDed57) {
                addError(result, 54, "DeductionUs57iia",
                    "Deduction u/s 57(iia) cannot exceed lower of 1/3rd of family pension or ₹15,000 (₹" + maxDed57 + ").");
            }
        }

        // Rule 57 & 58: Entertainment allowance 16(ii)
        if (result.entertainmentAlw16ii > 0) {
            EmployerCategory ec = EmployerCategory.fromCode(prefill.employerCategoryCode);
            if (!ec.isCentralOrStateGovt()) {
                addError(result, 58, "EntertainmentAlw16ii",
                    "Entertainment allowance u/s 16(ii) is allowed only for Central/State Government/PSU employees.");
            } else {
                long maxEntertain = Math.min(5_000, result.salary17_1 / 5);
                if (result.entertainmentAlw16ii > maxEntertain) {
                    addError(result, 57, "EntertainmentAlw16ii",
                        "Entertainment allowance cannot exceed ₹5,000 or 1/5th of salary (₹" + maxEntertain + "), whichever is lower.");
                }
            }
        }

        // Rule 60: Net Salary = Gross Salary - Allowances Exempt u/s 10 - Relief 89A
        long expectedNetSal = result.grossSalary - result.allowancesExemptUs10;
        if (result.netSalary != expectedNetSal) {
            addError(result, 60, "NetSalary",
                "Net Salary (₹" + result.netSalary + ") should equal Gross Salary - Allowances Exempt u/s 10 (₹" + expectedNetSal + ").");
        }

        // Rule 62: Income from Salary = Net Salary - Deductions u/s 16
        long expectedSalInc = result.netSalary - result.deductionUs16;
        if (result.incomeFromSalary != expectedSalInc) {
            addError(result, 62, "IncomeFromSalary",
                "Income from Salary (₹" + result.incomeFromSalary + ") should equal Net Salary - Deductions u/s 16 (₹" + expectedSalInc + ").");
        }

        // Rule 63: Exempt allowances u/s 10 ≤ Gross Salary
        if (result.allowancesExemptUs10 > result.grossSalary) {
            addError(result, 63, "AllowancesExemptUs10",
                "Total allowances exempt u/s 10 cannot exceed Gross Salary.");
        }

        // Rule 104: Total taxes paid = TDS + TCS + Advance Tax + SAT
        long computedTotalTaxPaid = result.tdsSalaryTotal + result.tdsOtherTotal
                + result.tcsTotal + result.advanceTaxTotal + result.selfAssessmentTaxTotal;
        if (result.totalTaxesPaid != computedTotalTaxPaid) {
            addError(result, 104, "TotalTaxesPaid",
                "Total Taxes Paid (₹" + result.totalTaxesPaid + ") should equal TDS + TCS + AT + SAT (₹" + computedTotalTaxPaid + ").");
        }

        // Rule 105: Refund = Total Taxes Paid - Total Tax & Interest Payable
        long expectedRefund = result.totalTaxesPaid - result.totalTaxFeeInterest;
        if (expectedRefund > 0 && result.refundDue != expectedRefund) {
            addError(result, 105, "RefundDue",
                "Refund (₹" + result.refundDue + ") should be ₹" + expectedRefund + " (Total Taxes Paid - Tax Due).");
        }

        // Rule 106: Tax payable = Total Tax & Interest - Total Taxes Paid
        long expectedTaxDue = result.totalTaxFeeInterest - result.totalTaxesPaid;
        if (expectedTaxDue > 0 && result.balTaxPayable != expectedTaxDue) {
            addError(result, 106, "BalTaxPayable",
                "Tax payable (₹" + result.balTaxPayable + ") should be ₹" + expectedTaxDue + ".");
        }

        // Rule 112: Old regime standard deduction u/s 16(ia) ≤ ₹50,000
        if (isOldRegime && result.stdDeductionUs16ia > 50_000) {
            addError(result, 112, "DeductionUs16ia",
                "Standard deduction u/s 16(ia) cannot exceed ₹50,000 under old regime.");
        }

        // Rule 115: 80CCD(1B) ≤ ₹50,000 (old regime)
        if (isOldRegime && result.sec80CCD_1B > 50_000) {
            addError(result, 115, "Section80CCD1B",
                "Deduction u/s 80CCD(1B) cannot exceed ₹50,000.");
        }

        // Rule 117: Total Income (excluding LTCG) ≤ ₹50 lakhs
        if (result.totalIncome > 5_000_000) {
            addError(result, 117, "TotalIncome",
                "Total income (₹" + result.totalIncome + ") exceeds ₹50 lakhs. ITR-1 is not applicable; please use ITR-2.");
        }

        // Rule 120: HRA claimed — 80GG not allowed
        if (prefill.hra10_13A > 0 && result.sec80GG > 0) {
            addError(result, 120, "Section80GG",
                "Deduction u/s 80GG is not allowed if HRA u/s 10(13A) is claimed.");
        }

        // Rule 122: 80EE ≤ ₹50,000 (old regime)
        if (isOldRegime && result.sec80EE > 50_000) {
            addError(result, 122, "Section80EE",
                "Deduction u/s 80EE cannot exceed ₹50,000.");
        }

        // Rule 123: 80EEA ≤ ₹1,50,000 (old regime)
        if (isOldRegime && result.sec80EEA > 150_000) {
            addError(result, 123, "Section80EEA",
                "Deduction u/s 80EEA cannot exceed ₹1,50,000.");
        }

        // Rule 124: Only one of 80EE / 80EEA
        if (result.sec80EE > 0 && result.sec80EEA > 0) {
            addError(result, 124, "Section80EE/80EEA",
                "Only one of deductions u/s 80EE or 80EEA is allowed, not both.");
        }

        // Rule 125: 80EEB ≤ ₹1,50,000 (old regime)
        if (isOldRegime && result.sec80EEB > 150_000) {
            addError(result, 125, "Section80EEB",
                "Deduction u/s 80EEB (EV loan) cannot exceed ₹1,50,000.");
        }

        // Rule 128: 80D self & family ≤ ₹25,000 (old regime)
        if (isOldRegime && prefill.sch80DDetail != null) {
            PreFillData.Sch80DDetail d = prefill.sch80DDetail;
            if (d.selfFamilyTotal > 25_000) {
                addError(result, 128, "Schedule80D",
                    "Deduction u/s 80D for Self & Family cannot exceed ₹25,000.");
            }
            if (d.selfFamilySrTotal > 50_000) {
                addError(result, 131, "Schedule80D",
                    "Deduction u/s 80D for Self & Family (Senior Citizen) cannot exceed ₹50,000.");
            }
            if (d.parentsTotal > 25_000) {
                addError(result, 133, "Schedule80D",
                    "Deduction u/s 80D for Parents cannot exceed ₹25,000.");
            }
            if (d.parentsSrTotal > 50_000) {
                addError(result, 135, "Schedule80D",
                    "Deduction u/s 80D for Parents (Senior Citizen) cannot exceed ₹50,000.");
            }
            if (d.eligibleAmount > 100_000) {
                addError(result, 137, "Schedule80D",
                    "Total eligible deduction u/s 80D cannot exceed ₹1,00,000.");
            }
            // Preventive health check-up across all fields ≤ ₹5,000
            long totalPreventive = d.selfFamilyPreventiveCheckup + d.parentsSrPreventiveCheckup
                    + d.parentsPreventiveCheckup;
            if (totalPreventive > 5_000) {
                addError(result, 130, "Schedule80D",
                    "Total preventive health check-up amount across all categories cannot exceed ₹5,000.");
            }
        }

        // Rule 147: New regime — Chapter VI-A deductions not allowed
        if (!isOldRegime) {
            if (result.sec80C > 0 || result.sec80CCC > 0 || result.sec80CCD_1 > 0
                    || result.sec80D > 0 || result.sec80E > 0 || result.sec80G > 0
                    || result.sec80TTA > 0 || result.sec80U > 0) {
                addError(result, 147, "ChapterVIA",
                    "Under New Tax Regime, deductions u/s 80C, 80CCC, 80CCD(1), 80D, 80E, 80G, 80TTA, 80U etc. are not allowed. " +
                    "Only 80CCD(2) and 80CCH are permitted.");
            }
        }

        // ── Category B Rules ──────────────────────────────────────────────────

        // Rule 113: TDS claimed but income not offered to tax
        if (result.tdsTotal > 0 && result.grossTotalIncome == 0) {
            addWarning(result, 113, "TDS",
                "TDS has been claimed but income has not been offered to tax. " +
                "Please ensure corresponding income is included in the return.");
        }

        // ── Category D Rules ──────────────────────────────────────────────────

        // Rule on 80GGA: not allowed if income includes business/profession
        if (result.sec80GGA > 0) {
            addInfo(result, 91, "Section80GGA",
                "Ensure details are provided in Schedule 80GGA. Deduction u/s 80GGA is not allowed if you have business/profession income.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addError(ITR1Result result, int rule, String field, String message) {
        result.errors.add(new ITR1Result.ValidationMessage(rule, "A", field, message));
    }

    private void addWarning(ITR1Result result, int rule, String field, String message) {
        result.warnings.add(new ITR1Result.ValidationMessage(rule, "B", field, message));
    }

    private void addInfo(ITR1Result result, int rule, String field, String message) {
        result.infos.add(new ITR1Result.ValidationMessage(rule, "D", field, message));
    }
}
