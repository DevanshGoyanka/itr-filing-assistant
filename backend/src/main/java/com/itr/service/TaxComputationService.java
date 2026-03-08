package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr1FormData.*;
import org.springframework.stereotype.Service;

/**
 * Tax Computation Engine — CBDT AY 2025-26 compliant.
 * Implements old regime + new regime (115BAC) slabs, 87A rebate, surcharge, cess.
 */
@Service
public class TaxComputationService {

    // ────────── Old Regime Slabs ──────────
    private static final double[][] OLD_SLABS = {
        {250000, 0.00},   // 0         → 2,50,000 = NIL
        {500000, 0.05},   // 2,50,001  → 5,00,000 = 5%
        {1000000, 0.20},  // 5,00,001  → 10,00,000 = 20%
        {Double.MAX_VALUE, 0.30}  // Above 10,00,000 = 30%
    };

    // ────────── New Regime Slabs (u/s 115BAC) ──────────
    private static final double[][] NEW_SLABS = {
        {300000, 0.00},   // 0         → 3,00,000 = NIL
        {700000, 0.05},   // 3,00,001  → 7,00,000 = 5%
        {1000000, 0.10},  // 7,00,001  → 10,00,000 = 10%
        {1200000, 0.15},  // 10,00,001 → 12,00,000 = 15%
        {1500000, 0.20},  // 12,00,001 → 15,00,000 = 20%
        {Double.MAX_VALUE, 0.30}  // Above 15,00,000 = 30%
    };

    // ────────── Old Regime: Rebate u/s 87A ──────────
    // Rule 201: Total income ≤ 5,00,000 → rebate = min(tax, 12500)
    private static final double OLD_REBATE_LIMIT = 500000;
    private static final double OLD_REBATE_MAX = 12500;

    // ────────── New Regime: Rebate u/s 87A ──────────
    // Rule 200: Total income ≤ 7,00,000 → rebate = min(tax, 25000)
    // Marginal relief for income between 7,00,001 and 7,22,230
    private static final double NEW_REBATE_LIMIT = 700000;
    private static final double NEW_REBATE_MAX = 25000;
    private static final double NEW_MARGINAL_RELIEF_LIMIT = 722230;

    // ────────── Cess = 4% ──────────
    private static final double CESS_RATE = 0.04;

    /**
     * Compute full tax for both regimes and populate the TaxComputation.
     */
    public TaxComputation computeTax(double grossTotalIncome, double totalDeductions,
                                     boolean isNewRegime) {
        double netTaxableIncome = Math.max(grossTotalIncome - totalDeductions, 0);
        // Round to nearest 10 (Rule: rounded off u/s 288A)
        long roundedIncome = roundToNearest10(netTaxableIncome);

        RegimeTaxBreakdown oldRegime = computeRegime("old", grossTotalIncome, totalDeductions);
        RegimeTaxBreakdown newRegime = computeRegime("new", grossTotalIncome, totalDeductions);

        String selectedRegime = isNewRegime ? "new" : "old";
        RegimeTaxBreakdown selected = isNewRegime ? newRegime : oldRegime;

        return TaxComputation.builder()
                .grossTotalIncome(grossTotalIncome)
                .totalDeductions(totalDeductions)
                .totalTaxableIncome(roundedIncome)
                .oldRegime(oldRegime)
                .newRegime(newRegime)
                .selectedRegime(selectedRegime)
                .taxOnIncome(selected.getTaxOnIncome())
                .rebateU87A(selected.getRebateU87A())
                .taxAfterRebate(selected.getTaxAfterRebate())
                .surcharge(selected.getSurcharge())
                .cessAt4Pct(selected.getCessAt4Pct())
                .totalTaxLiability(selected.getTotalTax())
                .build();
    }

    /**
     * Compute tax breakdown for a single regime.
     */
    public RegimeTaxBreakdown computeRegime(String regime, double grossTotalIncome,
                                             double totalDeductions) {
        double netTaxableIncome = Math.max(grossTotalIncome - totalDeductions, 0);
        long roundedIncome = roundToNearest10(netTaxableIncome);

        boolean isNew = "new".equals(regime);
        double[][] slabs = isNew ? NEW_SLABS : OLD_SLABS;

        // 1. Compute tax on slabs
        double taxOnIncome = computeSlabTax(roundedIncome, slabs);

        // 2. Rebate u/s 87A
        double rebate = 0;
        double marginalRelief = 0;
        if (isNew) {
            rebate = computeNewRegimeRebate(roundedIncome, taxOnIncome);
            if (roundedIncome > NEW_REBATE_LIMIT && roundedIncome <= NEW_MARGINAL_RELIEF_LIMIT) {
                // Marginal relief: tax cannot exceed income exceeding 7L [Rule 200]
                double excessIncome = roundedIncome - NEW_REBATE_LIMIT;
                double taxWithoutRebate = taxOnIncome;
                if (taxWithoutRebate > excessIncome) {
                    marginalRelief = taxWithoutRebate - excessIncome;
                    taxOnIncome = excessIncome;
                    rebate = 0; // Marginal relief replaces rebate
                }
            }
        } else {
            rebate = computeOldRegimeRebate(roundedIncome, taxOnIncome);
        }

        double taxAfterRebate = Math.max(taxOnIncome - rebate, 0);

        // 3. Surcharge (ITR-1 cap is 50L, so 10% surcharge for 50L-1Cr, but rarely applies)
        double surcharge = computeSurcharge(roundedIncome, taxAfterRebate);

        // 4. Health & Education Cess = 4%
        double cess = Math.round((taxAfterRebate + surcharge) * CESS_RATE);

        double totalTax = taxAfterRebate + surcharge + cess;

        return RegimeTaxBreakdown.builder()
                .regime(regime)
                .taxableIncome(roundedIncome)
                .taxOnIncome(isNew && marginalRelief > 0 ? taxOnIncome : computeSlabTax(roundedIncome, slabs))
                .rebateU87A(rebate)
                .taxAfterRebate(taxAfterRebate)
                .surcharge(surcharge)
                .cessAt4Pct(cess)
                .totalTax(totalTax)
                .marginalRelief(marginalRelief)
                .build();
    }

    /**
     * Calculate slab-wise tax.
     */
    private double computeSlabTax(double income, double[][] slabs) {
        double tax = 0;
        double prevLimit = 0;
        for (double[] slab : slabs) {
            double limit = slab[0];
            double rate = slab[1];
            if (income <= prevLimit) break;
            double taxableInSlab = Math.min(income, limit) - prevLimit;
            tax += taxableInSlab * rate;
            prevLimit = limit;
        }
        return Math.round(tax);
    }

    /**
     * Old regime rebate: if total income ≤ 5L, rebate = min(tax, 12500) [Rule 201]
     */
    private double computeOldRegimeRebate(double income, double tax) {
        if (income <= OLD_REBATE_LIMIT) {
            return Math.min(tax, OLD_REBATE_MAX);
        }
        return 0;
    }

    /**
     * New regime rebate: if total income ≤ 7L, rebate = min(tax, 25000) [Rule 200]
     */
    private double computeNewRegimeRebate(double income, double tax) {
        if (income <= NEW_REBATE_LIMIT) {
            return Math.min(tax, NEW_REBATE_MAX);
        }
        return 0;
    }

    /**
     * Surcharge for ITR-1 (income ≤ 50L).
     * Income 50L-1Cr: 10% (but ITR-1 is capped at 50L so this is edge-case only)
     */
    private double computeSurcharge(double income, double tax) {
        // For ITR-1 filers (income ≤ 50L): no surcharge normally applies.
        // Including for completeness.
        if (income > 5000000) {
            return Math.round(tax * 0.10);
        }
        return 0;
    }

    /**
     * Round to nearest 10 as per section 288A.
     */
    private long roundToNearest10(double amount) {
        return Math.round(amount / 10.0) * 10;
    }
}
