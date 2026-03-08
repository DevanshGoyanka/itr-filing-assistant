package com.itr1.calculator.service;

import com.itr1.calculator.model.*;
import com.itr1.calculator.model.PreFillData.TaxPaymentEntry;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Core ITR-1 calculation engine for AY 2025-26.
 *
 * Orchestrates:
 *  1. Salary income computation (B1)
 *  2. House property income (B2)
 *  3. Other sources income (B3)
 *  4. Gross Total Income
 *  5. Chapter VI-A deductions (with limits)
 *  6. Total Income
 *  7. Tax computation (slabs, rebate, cess)
 *  8. Interest u/s 234A/B/C and fee u/s 234F
 *  9. Taxes paid reconciliation
 * 10. Refund / balance tax payable
 * 11. CBDT Validation Rules AY 2025-26 V1.1
 */
@Service
public class ITR1CalculatorService {

    private final TaxSlabCalculator      slabCalc;
    private final InterestCalculator     interestCalc;
    private final ITR1ValidationService  validationService;

    public ITR1CalculatorService(TaxSlabCalculator slabCalc,
                                  InterestCalculator interestCalc,
                                  ITR1ValidationService validationService) {
        this.slabCalc         = slabCalc;
        this.interestCalc     = interestCalc;
        this.validationService = validationService;
    }

    /**
     * Compute complete ITR-1 from prefill data.
     *
     * @param prefill     Data extracted from portal prefill JSON
     * @param filingDate  Expected filing date (yyyy-MM-dd) — used for 234F / 234A
     */
    public ITR1Result compute(PreFillData prefill, String filingDate) {
        ITR1Result result = new ITR1Result();

        TaxRegime regime = prefill.optingNewTaxRegime == 2 ? TaxRegime.NEW_REGIME : TaxRegime.OLD_REGIME;
        result.taxRegime = regime;

        int ageYears = computeAge(prefill.dob);

        // ── Step 1: Salary Income (Schedule B1) ──────────────────────────────

        computeSalaryIncome(prefill, result, regime);

        // ── Step 2: House Property Income (Schedule B2) ──────────────────────

        computeHousePropertyIncome(prefill, result, regime);

        // ── Step 3: Other Sources Income (Schedule B3) ───────────────────────

        computeOtherSourcesIncome(prefill, result, regime);

        // ── Step 4: Gross Total Income ────────────────────────────────────────

        result.grossTotalIncome = result.incomeFromSalary
                + result.totalIncomeOfHP
                + result.netIncomeOthSrc;
        if (result.grossTotalIncome < 0) result.grossTotalIncome = 0;

        // ── Step 5: Chapter VI-A Deductions (capped at GTI) ──────────────────

        computeChapterVIADeductions(prefill, result, regime, ageYears);

        // ── Step 6: Total Income ──────────────────────────────────────────────

        result.totalIncome = Math.max(0, result.grossTotalIncome - result.totalChapVIADeductions);

        // ── Step 7: Tax Computation ───────────────────────────────────────────

        computeTax(prefill, result, regime, ageYears);

        // ── Step 8: Interest & Fees ───────────────────────────────────────────

        computeInterestAndFees(prefill, result, filingDate);

        // ── Step 9: Taxes Paid ────────────────────────────────────────────────

        computeTaxesPaid(prefill, result, filingDate);

        // ── Step 10: Refund / Balance Tax ─────────────────────────────────────

        long balance = result.totalTaxFeeInterest - result.totalTaxesPaid;
        if (balance > 0) {
            result.balTaxPayable = balance;
            result.refundDue     = 0;
        } else {
            result.balTaxPayable = 0;
            result.refundDue     = Math.abs(balance);
        }

        // ── Step 11: CBDT Validation Rules ────────────────────────────────────

        validationService.validate(prefill, result, ageYears);

        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 1: Salary Income
    // ──────────────────────────────────────────────────────────────────────────

    private void computeSalaryIncome(PreFillData p, ITR1Result r, TaxRegime regime) {
        r.salary17_1       = p.salary17_1;
        r.perquisites17_2  = p.perquisites17_2;
        r.profitsInLieu17_3 = p.profitsInLieu17_3;

        // Gross Salary = 17(1) + 17(2) + 17(3)
        r.grossSalary = r.salary17_1 + r.perquisites17_2 + r.profitsInLieu17_3;

        // Allowances exempt u/s 10
        long totalExempt = p.hra10_13A + p.lta10_5 + p.otherExempt10_14_i
                + p.otherExempt10_14_ii + p.gratuity10_10
                + p.leaveEncashment10_10AA + p.vrsAmount10_10C;

        if (p.totalAllwncExemptUs10 > 0) {
            totalExempt = p.totalAllwncExemptUs10; // prefer pre-computed value
        }

        // HRA (old regime only — new regime doesn't allow HRA exemption)
        if (regime == TaxRegime.NEW_REGIME && p.hra10_13A > 0) {
            totalExempt -= p.hra10_13A;  // HRA not exempt under new regime
        }

        // Cap exempt allowances at gross salary
        r.allowancesExemptUs10 = Math.min(totalExempt, r.grossSalary);

        // Net Salary
        r.netSalary = r.grossSalary - r.allowancesExemptUs10;

        // Standard deduction u/s 16(ia)
        long stdDed = slabCalc.standardDeduction(regime);
        r.stdDeductionUs16ia = Math.min(stdDed, r.netSalary);

        // Entertainment allowance u/s 16(ii)
        if (regime == TaxRegime.OLD_REGIME) {
            EmployerCategory ec = EmployerCategory.fromCode(p.employerCategoryCode);
            if (ec.isCentralOrStateGovt()) {
                long maxEntertain = Math.min(5_000, r.salary17_1 / 5);
                r.entertainmentAlw16ii = Math.min(p.totalAllwncExemptUs10 > 0 ? 0 : maxEntertain, maxEntertain);
            }
        }

        // Professional tax u/s 16(iii) — actual paid, max ₹2,500 per year
        r.professionalTaxUs16iii = Math.min(p.professionalTaxUs16iii, 2_500);

        r.deductionUs16 = r.stdDeductionUs16ia + r.entertainmentAlw16ii + r.professionalTaxUs16iii;

        r.incomeFromSalary = Math.max(0, r.netSalary - r.deductionUs16);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 2: House Property Income
    // ──────────────────────────────────────────────────────────────────────────

    private void computeHousePropertyIncome(PreFillData p, ITR1Result r, TaxRegime regime) {
        r.typeOfHP         = p.typeOfHP;
        r.grossRentReceived = p.grossRentReceived;
        r.taxPaidLocalAuth  = "S".equalsIgnoreCase(p.typeOfHP) ? 0 : p.taxPaidLocalAuth;

        r.annualValue = Math.max(0, r.grossRentReceived - r.taxPaidLocalAuth);

        if ("S".equalsIgnoreCase(p.typeOfHP)) {
            // Self-Occupied: Annual Value = 0
            r.annualValue       = 0;
            r.standardDeductionHP = 0;
            // Interest on borrowed capital — max ₹2L (old), ₹2L (new for SO)
            long maxInterest = regime == TaxRegime.OLD_REGIME ? 200_000 : 200_000;
            r.interestPayable = Math.min(p.interestOnBorrowedCapital, maxInterest);
            r.totalIncomeOfHP = -r.interestPayable; // Loss from HP
        } else {
            // Let-out or Deemed Let-out
            r.standardDeductionHP = r.annualValue * 30 / 100;
            r.interestPayable     = p.interestOnBorrowedCapital;
            r.arrearsUnrealizedRent = p.arrearsUnrealizedRentRcvd;
            r.totalIncomeOfHP = r.annualValue - r.standardDeductionHP
                    - r.interestPayable + r.arrearsUnrealizedRent;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 3: Other Sources Income
    // ──────────────────────────────────────────────────────────────────────────

    private void computeOtherSourcesIncome(PreFillData p, ITR1Result r, TaxRegime regime) {
        r.savingsAccountInterest = p.savingsAccountInterest;
        r.fdInterest             = p.fdInterest;
        r.dividendIncome         = p.dividendIncome;
        r.familyPension          = p.familyPension;
        r.otherIncome            = p.otherIncome;

        r.incomeOthSrc = r.savingsAccountInterest + r.fdInterest
                + r.dividendIncome + r.familyPension + r.otherIncome;

        // Deduction u/s 57(iia) — family pension: min(1/3rd, ₹15,000)
        if (r.familyPension > 0 && regime == TaxRegime.OLD_REGIME) {
            r.deductionUs57iia = Math.min(r.familyPension / 3, 15_000);
        }

        r.netIncomeOthSrc = Math.max(0, r.incomeOthSrc - r.deductionUs57iia);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 5: Chapter VI-A Deductions
    // ──────────────────────────────────────────────────────────────────────────

    private void computeChapterVIADeductions(PreFillData p, ITR1Result r,
                                               TaxRegime regime, int ageYears) {
        long gti = r.grossTotalIncome;

        if (regime == TaxRegime.NEW_REGIME) {
            // New regime: only 80CCD(2) and 80CCH allowed
            r.sec80CCD_2 = capAt(p.section80CCD_Employer, r.salary17_1 * 14 / 100);
            r.sec80CCH   = p.section80CCH;
            r.totalChapVIADeductions = cap(r.sec80CCD_2 + r.sec80CCH, gti);
            return;
        }

        // ── Old Regime ────────────────────────────────────────────────────────

        // 80C + 80CCC + 80CCD(1) capped at ₹1,50,000
        long raw80CGroup = p.section80C + p.section80CCC + p.section80CCD_Employee;
        long capped80CGroup = Math.min(raw80CGroup, 150_000);

        // Apportion the cap proportionally
        if (raw80CGroup > 0) {
            double ratio = (double) capped80CGroup / raw80CGroup;
            r.sec80C      = Math.round(p.section80C            * ratio);
            r.sec80CCC    = Math.round(p.section80CCC          * ratio);
            r.sec80CCD_1  = Math.round(p.section80CCD_Employee * ratio);
        }

        // 80CCD(1B) — additional NPS, max ₹50,000
        r.sec80CCD_1B = capAt(p.section80CCD_1B, 50_000);

        // 80CCD(2) — employer contribution, max 10% (or 14% CG/SG) of salary
        EmployerCategory ec = EmployerCategory.fromCode(p.employerCategoryCode);
        long maxCCD2 = ec.isCentralOrStateGovt()
                ? r.salary17_1 * 14 / 100
                : r.salary17_1 * 10 / 100;
        r.sec80CCD_2 = ec.isPensioner() ? 0 : capAt(p.section80CCD_Employer, maxCCD2);

        // 80D
        r.sec80D = computeSection80D(p, ageYears, gti);

        // 80DD — disability of dependent
        r.sec80DD = p.section80DD; // amount validated by Schedule 80DD

        // 80DDB — specified disease
        long maxDDB = ageYears >= 60 ? 100_000 : 40_000;
        r.sec80DDB = capAt(p.section80DDB, maxDDB);

        // 80E — education loan interest (no limit)
        r.sec80E = p.section80E;

        // 80EE / 80EEA (mutually exclusive)
        if (p.section80EEA > 0) {
            r.sec80EEA = capAt(p.section80EEA, 150_000);
            r.sec80EE  = 0;
        } else {
            r.sec80EE  = capAt(p.section80EE, 50_000);
        }

        // 80EEB — EV loan, max ₹1,50,000
        r.sec80EEB = capAt(p.section80EEB, 150_000);

        // 80G — donations (capped at GTI)
        r.sec80G   = capAt(p.section80G,   gti);

        // 80GG — rent if no HRA; max min(₹60,000, 25% of (GTI - LTCG))
        if (p.hra10_13A == 0) {
            long maxGG = Math.min(60_000, gti * 25 / 100);
            r.sec80GG = capAt(p.section80GG, maxGG);
        }

        // 80GGA — scientific research/rural development donations
        r.sec80GGA = capAt(p.section80GGA, gti);

        // 80GGC — political party donations
        r.sec80GGC = capAt(p.section80GGC, gti);

        // 80TTA — savings bank interest, max ₹10,000 (only if < 60)
        if (ageYears < 60) {
            long maxTTA = Math.min(10_000, r.savingsAccountInterest);
            r.sec80TTA = capAt(p.section80TTA, maxTTA);
        }

        // 80TTB — all interest for senior citizens, max ₹50,000
        if (ageYears >= 60) {
            long totalInterest = r.savingsAccountInterest + r.fdInterest;
            r.sec80TTB = capAt(p.section80TTB, Math.min(50_000, totalInterest));
        }

        // 80U — self disability
        r.sec80U = p.section80U;

        // Total Chapter VI-A (cannot exceed GTI)
        long total = r.sec80C + r.sec80CCC + r.sec80CCD_1 + r.sec80CCD_1B + r.sec80CCD_2
                + r.sec80D + r.sec80DD + r.sec80DDB
                + r.sec80E + r.sec80EE + r.sec80EEA + r.sec80EEB
                + r.sec80G + r.sec80GG + r.sec80GGA + r.sec80GGC
                + r.sec80TTA + r.sec80TTB + r.sec80U + r.sec80CCH;

        r.totalChapVIADeductions = cap(total, gti);
    }

    /**
     * Compute Schedule 80D eligible deduction.
     */
    private long computeSection80D(PreFillData p, int ageYears, long gti) {
        if (p.sch80DDetail != null) {
            PreFillData.Sch80DDetail d = p.sch80DDetail;
            long selfFamily = ageYears >= 60
                    ? Math.min(d.selfFamilySrTotal, 50_000)
                    : Math.min(d.selfFamilyTotal,   25_000);
            long parents    = Math.min(d.parentsSrTotal > 0 ? d.parentsSrTotal : d.parentsTotal,
                                       d.parentsSrTotal > 0 ? 50_000 : 25_000);
            long total      = selfFamily + parents;
            return Math.min(total, Math.min(100_000, gti));
        }
        // Fallback to simple value
        return capAt(p.section80D, Math.min(100_000, gti));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 7: Tax Computation
    // ──────────────────────────────────────────────────────────────────────────

    private void computeTax(PreFillData p, ITR1Result r, TaxRegime regime, int ageYears) {
        long taxableIncome = r.totalIncome;

        if (regime == TaxRegime.NEW_REGIME) {
            r.totalTaxPayable = slabCalc.computeNewRegimeTax(taxableIncome, ageYears);
            r.rebate87A       = slabCalc.computeRebate87ANewRegime(taxableIncome, r.totalTaxPayable);
        } else {
            r.totalTaxPayable = slabCalc.computeOldRegimeTax(taxableIncome, ageYears);
            r.rebate87A       = slabCalc.computeRebate87AOldRegime(taxableIncome, r.totalTaxPayable);
        }

        r.taxAfterRebate   = Math.max(0, r.totalTaxPayable - r.rebate87A);
        r.educationCess    = r.taxAfterRebate * 4 / 100;
        r.grossTaxLiability = r.taxAfterRebate + r.educationCess;

        // Relief u/s 89 (arrears)
        r.reliefUs89        = p.reliefUs89;
        r.netTaxLiability   = Math.max(0, r.grossTaxLiability - r.reliefUs89);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 8: Interest & Fees
    // ──────────────────────────────────────────────────────────────────────────

    private void computeInterestAndFees(PreFillData p, ITR1Result r, String filingDate) {
        r.lateFilingFee234F = interestCalc.fee234F(filingDate, r.totalIncome);

        // Advance tax paid (payments before March 31, 2025)
        long advanceTaxPaid = sumAdvanceTax(p.taxPayments);
        r.intrstPayUs234B   = interestCalc.interest234B(r.netTaxLiability, advanceTaxPaid, filingDate);
        r.intrstPayUs234C   = interestCalc.interest234C(r.netTaxLiability, p.taxPayments);
        r.intrstPayUs234A   = interestCalc.interest234A(
                Math.max(0, r.netTaxLiability - advanceTaxPaid), filingDate);

        r.totalInterestFee   = r.intrstPayUs234A + r.intrstPayUs234B
                + r.intrstPayUs234C + r.lateFilingFee234F;
        r.totalTaxFeeInterest = r.netTaxLiability + r.totalInterestFee;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 9: Taxes Paid
    // ──────────────────────────────────────────────────────────────────────────

    private void computeTaxesPaid(PreFillData p, ITR1Result r, String filingDate) {
        // TDS on salary
        r.tdsSalaryTotal = 0;
        if (p.tdsOnSalary != null) {
            for (PreFillData.TdsEntry e : p.tdsOnSalary) {
                r.tdsSalaryTotal += e.taxClaimed;
            }
        }

        // TDS on other income
        r.tdsOtherTotal = 0;
        if (p.tdsOnOtherIncome != null) {
            for (PreFillData.TdsEntry e : p.tdsOnOtherIncome) {
                r.tdsOtherTotal += e.taxClaimed;
            }
        }

        r.tdsTotal = r.tdsSalaryTotal + r.tdsOtherTotal;

        // TCS
        r.tcsTotal = 0;
        if (p.tcsEntries != null) {
            for (PreFillData.TcsEntry e : p.tcsEntries) {
                r.tcsTotal += e.taxClaimed;
            }
        }

        // Advance tax vs self-assessment tax split
        LocalDate fyEnd = LocalDate.of(2025, 3, 31);
        r.advanceTaxTotal        = 0;
        r.selfAssessmentTaxTotal = 0;
        if (p.taxPayments != null) {
            for (TaxPaymentEntry e : p.taxPayments) {
                LocalDate d = parseDate(e.dateOfDeposit);
                if (d != null && !d.isAfter(fyEnd)) {
                    r.advanceTaxTotal += e.amount;
                } else {
                    r.selfAssessmentTaxTotal += e.amount;
                }
            }
        }

        r.totalTaxesPaid = r.tdsTotal + r.tcsTotal
                + r.advanceTaxTotal + r.selfAssessmentTaxTotal;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility helpers
    // ──────────────────────────────────────────────────────────────────────────

    private int computeAge(String dob) {
        if (dob == null || dob.isBlank()) return 30;
        try {
            LocalDate birth = LocalDate.parse(dob);
            // Age as of April 1, 2024 (start of FY 2024-25)
            LocalDate fyStart = LocalDate.of(2024, 4, 1);
            return (int) (fyStart.toEpochDay() - birth.toEpochDay()) / 365;
        } catch (Exception e) {
            return 30;
        }
    }

    private long sumAdvanceTax(List<TaxPaymentEntry> payments) {
        if (payments == null) return 0;
        LocalDate fyEnd = LocalDate.of(2025, 3, 31);
        return payments.stream()
                .filter(e -> {
                    LocalDate d = parseDate(e.dateOfDeposit);
                    return d != null && !d.isAfter(fyEnd);
                })
                .mapToLong(e -> e.amount)
                .sum();
    }

    private long capAt(long value, long max) {
        return Math.min(Math.max(0, value), Math.max(0, max));
    }

    private long cap(long value, long gti) {
        return Math.min(Math.max(0, value), Math.max(0, gti));
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try { return LocalDate.parse(date); } catch (Exception e) { return null; }
    }
}
