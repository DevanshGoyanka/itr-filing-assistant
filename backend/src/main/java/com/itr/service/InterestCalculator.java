package com.itr.service;

import com.itr.model.PreFillData.TaxPaymentEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Calculates interest and fees under sections 234A, 234B, 234C, and 234F
 * for AY 2025-26 (FY 2024-25).
 *
 * These are approximate calculations suitable for pre-computation.
 * Exact amounts are determined by the CPC.
 *
 * FY 2024-25: April 1, 2024 – March 31, 2025
 * Due date (ITR-1, non-audit): July 31, 2025
 */
@Component
public class InterestCalculator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── 234F: Late Filing Fee ─────────────────────────────────────────────────

    /**
     * Fee u/s 234F for late filing.
     * - Filed by July 31, 2025 (due date): ₹0
     * - Filed after due date, income > ₹5L: ₹5,000
     * - Filed after due date, income ≤ ₹5L: ₹1,000
     *
     * @param filingDate   actual or expected date of filing (yyyy-MM-dd), null = today's date
     * @param totalIncome  total income after deductions
     */
    public long fee234F(String filingDate, long totalIncome) {
        LocalDate dueDate = LocalDate.of(2025, 7, 31);
        LocalDate filing  = parseDate(filingDate);
        if (filing == null || !filing.isAfter(dueDate)) return 0;
        return totalIncome > 500_000 ? 5_000 : 1_000;
    }

    // ── 234A: Interest for Late Filing ────────────────────────────────────────

    /**
     * Interest u/s 234A: 1% per month (or part thereof) on tax due,
     * from August 1, 2025 to date of filing.
     *
     * @param taxDue       netTaxLiability (after TDS/advance tax set-off)
     * @param filingDate   actual filing date (yyyy-MM-dd)
     */
    public long interest234A(long taxDue, String filingDate) {
        if (taxDue <= 0) return 0;
        LocalDate dueDate  = LocalDate.of(2025, 7, 31);
        LocalDate filing   = parseDate(filingDate);
        if (filing == null || !filing.isAfter(dueDate)) return 0;

        int months = monthsBetween(dueDate, filing);
        return taxDue * 1 * months / 100;
    }

    // ── 234B: Interest for Default in Advance Tax ─────────────────────────────

    /**
     * Interest u/s 234B: 1% per month from April 1, 2025 to date of filing,
     * if advance tax paid < 90% of assessed tax.
     *
     * @param assessedTax      netTaxLiability
     * @param advanceTaxPaid   total advance tax paid
     * @param filingDate       filing date (yyyy-MM-dd)
     */
    public long interest234B(long assessedTax, long advanceTaxPaid, String filingDate) {
        long shortfall = assessedTax - advanceTaxPaid;
        if (shortfall <= 0) return 0;
        if (advanceTaxPaid >= assessedTax * 90 / 100) return 0;

        LocalDate start  = LocalDate.of(2025, 4, 1);
        LocalDate filing = parseDate(filingDate);
        if (filing == null) filing = LocalDate.now();

        int months = monthsBetween(start, filing);
        if (months <= 0) months = 1;
        return shortfall * 1 * months / 100;
    }

    // ── 234C: Interest for Deferment of Advance Tax ───────────────────────────

    /**
     * Interest u/s 234C: 1% per month for 3 months on shortfall at each
     * advance tax instalment date (simplified computation).
     *
     * Instalment schedule for AY 2025-26 (FY 2024-25):
     *   June 15, 2024   → 15% of tax due
     *   Sep 15, 2024    → 45% of tax due
     *   Dec 15, 2024    → 75% of tax due
     *   Mar 15, 2025    → 100% of tax due
     *
     * @param assessedTax   netTaxLiability
     * @param payments      list of actual advance tax payments
     */
    public long interest234C(long assessedTax, List<TaxPaymentEntry> payments) {
        if (assessedTax <= 0 || payments == null) return 0;

        long paid_jun = sumPaidByDate(payments, LocalDate.of(2024, 6, 15));
        long paid_sep = sumPaidByDate(payments, LocalDate.of(2024, 9, 15));
        long paid_dec = sumPaidByDate(payments, LocalDate.of(2024, 12, 15));
        long paid_mar = sumPaidByDate(payments, LocalDate.of(2025, 3, 15));

        long interest = 0;

        // June instalment: 15%
        long req_jun = assessedTax * 15 / 100;
        if (paid_jun < req_jun) {
            interest += (req_jun - paid_jun) * 1 * 3 / 100;
        }

        // September instalment: 45%
        long req_sep = assessedTax * 45 / 100;
        if (paid_sep < req_sep) {
            interest += (req_sep - paid_sep) * 1 * 3 / 100;
        }

        // December instalment: 75%
        long req_dec = assessedTax * 75 / 100;
        if (paid_dec < req_dec) {
            interest += (req_dec - paid_dec) * 1 * 3 / 100;
        }

        // March instalment: 100%
        long req_mar = assessedTax;
        if (paid_mar < req_mar) {
            interest += (req_mar - paid_mar) * 1 * 1 / 100; // 1 month
        }

        return interest;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long sumPaidByDate(List<TaxPaymentEntry> payments, LocalDate cutoff) {
        long sum = 0;
        for (TaxPaymentEntry p : payments) {
            LocalDate d = parseDate(p.dateOfDeposit);
            if (d != null && !d.isAfter(cutoff)) {
                sum += p.amount;
            }
        }
        return sum;
    }

    private int monthsBetween(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || to.isEqual(from)) return 0;
        int months = 0;
        LocalDate cur = from;
        while (cur.isBefore(to)) {
            cur = cur.plusMonths(1);
            months++;
        }
        return months;
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            // Handle "yyyy-MM-dd" and "dd/MM/yyyy"
            if (date.contains("/")) {
                String[] parts = date.split("/");
                if (parts.length == 3) {
                    return LocalDate.of(
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[0]));
                }
            }
            return LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
