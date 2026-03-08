package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete ITR-1 (Sahaj) Form Data for AY 2025-26.
 * Compliant with CBDT e-Filing Validation Rules V1.1.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Itr1FormData {

    private PartA_GeneralInfo partA;
    private ScheduleSalary scheduleSalary;
    private ScheduleHouseProperty scheduleHP;
    private ScheduleOtherSources scheduleOS;
    private ScheduleExemptIncome scheduleExempt;
    private DeductionsVIA deductionsVIA;
    private TaxComputation computation;
    private ScheduleTaxesPaid taxesPaid;
    @Builder.Default private List<String> validationErrors = new ArrayList<>();
    @Builder.Default private List<String> validationWarnings = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════
    // Part A — General Information
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PartA_GeneralInfo {
        private String assesseeName;
        private String pan;
        private String aadhaar;
        private String dob;            // yyyy-MM-dd
        private String email;
        private String mobile;
        private String address;
        @Builder.Default
        private String assessmentYear = "AY2025-26";
        @Builder.Default
        private String filingStatus = "original";          // original/revised/belated
        @Builder.Default
        private String residentialStatus = "resident";     // resident/nri
        @Builder.Default
        private boolean newTaxRegime = true;               // true = 115BAC new regime
        @Builder.Default
        private String natureOfEmployment = "others";      // CG/SG/PSU/others/pensioner/not_applicable
        @Builder.Default
        private String filingSection = "139(1)";
        // Employer details for PDF report (populated from prefill)
        @Builder.Default private String employerName = "";
        @Builder.Default private String employerTAN = "";
        @Builder.Default private String fathersName = "";
    }

    // ═══════════════════════════════════════════════════════════════
    // Schedule Salary — Income under head "Salaries"
    // Rules 59-77, 112, 224, 193
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScheduleSalary {
        // B1(ia) Salary as per sec 17(1)
        @Builder.Default private double salaryU17_1 = 0;
        // B1(ib) Value of perquisites as per sec 17(2)
        @Builder.Default private double perquisitesU17_2 = 0;
        // B1(ic) Profits in lieu of salary as per sec 17(3)
        @Builder.Default private double profitsU17_3 = 0;
        // B1(id) Income from retirement benefit account u/s 89A (notified country)
        @Builder.Default private double incomeU89A_notified = 0;
        // B1(ie) Income from retirement benefit account u/s 89A (other country)
        @Builder.Default private double incomeU89A_other = 0;

        // === Computed: B1(ii) Gross Salary = (ia + ib + ic + id + ie) [Rule 59] ===
        @Builder.Default private double grossSalary = 0;

        // B1(iia) Allowances exempt u/s 10 (total)
        @Builder.Default private double allowancesExemptU10 = 0;
        // B1(iib) Relief u/s 89A
        @Builder.Default private double reliefU89A = 0;

        // === Computed: B1(iii) Net Salary = Gross - Exempt - Relief [Rule 60] ===
        @Builder.Default private double netSalary = 0;

        // B1(iva) Standard deduction u/s 16(ia)
        // Old regime: max 50000 [Rule 112], New regime: max 75000 [Rule 224]
        @Builder.Default private double standardDeduction = 0;
        // B1(ivb) Entertainment allowance u/s 16(ii) — Old regime CG/SG/PSU only [Rules 57,58,164]
        @Builder.Default private double entertainmentAllowance = 0;
        // B1(ivc) Professional tax u/s 16(iii) — Old regime only [Rule 169]
        @Builder.Default private double professionalTax = 0;

        // === Computed: B1(iv) Total deductions u/s 16 = (iva+ivb+ivc) [Rule 61] ===
        @Builder.Default private double totalDeductionsU16 = 0;

        // === Computed: B1(v) Income chargeable under Salaries = (iii - iv) [Rule 62] ===
        @Builder.Default private double incomeFromSalary = 0;

        // Exempt allowances breakdown (for validation)
        @Builder.Default
        private List<ExemptAllowance> exemptAllowances = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExemptAllowance {
        private String section;
        private String description;
        @Builder.Default private double amount = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Schedule HP — Income from House Property
    // Rules 43-49, 161, 163, 250, 263
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScheduleHouseProperty {
        @Builder.Default
        private String propertyType = "self_occupied";  // self_occupied / let_out / deemed_let_out

        // B2(i) Gross Rent
        @Builder.Default private double grossRent = 0;
        // B2(ii) Municipal tax paid
        @Builder.Default private double municipalTaxPaid = 0;
        // === Computed: B2(iii) Annual Value = B2(i) - B2(ii) [Rule 46] ===
        @Builder.Default private double annualValue = 0;
        // === Computed: B2(iv) Standard deduction = 30% of Annual Value [Rule 43] ===
        @Builder.Default private double standardDeduction30Pct = 0;
        // B2(v) Interest on borrowed capital u/s 24(b)
        @Builder.Default private double interestOnLoanU24b = 0;
        // B2(vi) Arrears/unrealised rent received
        @Builder.Default private double arrearsUnrealizedRent = 0;
        // === Computed: B2(vii) Income from HP = (iii - iv - v + vi) [Rule 47] ===
        @Builder.Default private double incomeFromHP = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Schedule OS — Income from Other Sources
    // Rules 50-56, 146, 189-190
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScheduleOtherSources {
        @Builder.Default private double savingsInterest = 0;
        @Builder.Default private double depositInterest = 0;
        @Builder.Default private double incomeTaxRefundInterest = 0;
        @Builder.Default private double familyPension = 0;
        @Builder.Default private double dividendIncome = 0;
        @Builder.Default private double otherIncome = 0;
        @Builder.Default private double incomeU89A = 0;

        // === Computed: B3(i) Total of Other Sources [Rule 52] ===
        @Builder.Default private double grossOtherSources = 0;

        // B3(ii) Deduction u/s 57(iia) Family pension deduction
        // Old regime: min(1/3 of family pension, 15000) [Rule 54]
        // New regime: min(1/3 of family pension, 25000) [Rule 223]
        @Builder.Default private double deductionU57iia = 0;

        // === Computed: B3(iii) = B3(i) - B3(ii) ===
        @Builder.Default private double incomeFromOtherSources = 0;

        // Quarterly breakup of dividend [Rule 146]
        @Builder.Default private double dividendQ1 = 0;
        @Builder.Default private double dividendQ2 = 0;
        @Builder.Default private double dividendQ3 = 0;
        @Builder.Default private double dividendQ4 = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Schedule Exempt Income
    // Rules 226-227
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScheduleExemptIncome {
        @Builder.Default private double agricultureIncome = 0;
        @Builder.Default private double exemptInterestIncome = 0;
        @Builder.Default private double ltcgU112A_exempt = 0;    // max 125000 [Rule 226]
        @Builder.Default private double ltcgU112A_cost = 0;
        @Builder.Default private double ltcgU112A_sale = 0;
        @Builder.Default private double otherExemptIncome = 0;
        @Builder.Default private double totalExemptIncome = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Part C — Deductions under Chapter VI-A
    // Rules 115-125, 147, 154-160, 170-176, 195-196, 209-215
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DeductionsVIA {
        @Builder.Default private double section80C = 0;
        @Builder.Default private double section80CCC = 0;
        @Builder.Default private double section80CCD1 = 0;
        // === Computed: total 80C+80CCC+80CCD(1), max 150000 ===
        @Builder.Default private double total80C_CCC_CCD1 = 0;

        @Builder.Default private double section80CCD1B = 0;  // max 50000 [Rule 115]
        @Builder.Default private double section80CCD2 = 0;   // allowed in both regimes

        @Builder.Default private double section80D = 0;      // [Rules 128-139]
        @Builder.Default private double section80DD = 0;     // [Rules 212-215]
        @Builder.Default private double section80DDB = 0;    // [Rule 156, 249]
        @Builder.Default private double section80E = 0;
        @Builder.Default private double section80EE = 0;     // max 50000 [Rule 122]
        @Builder.Default private double section80EEA = 0;    // max 150000 [Rule 123]
        @Builder.Default private double section80EEB = 0;    // max 150000 [Rule 125]
        @Builder.Default private double section80G = 0;
        @Builder.Default private double section80GG = 0;     // max 60000 [Rule 114]
        @Builder.Default private double section80GGA = 0;
        @Builder.Default private double section80GGC = 0;
        @Builder.Default private double section80TTA = 0;    // max 10000
        @Builder.Default private double section80TTB = 0;    // max 50000
        @Builder.Default private double section80U = 0;
        @Builder.Default private double section80CCH = 0;    // [Rules 195-196]

        // === Computed: Total Deductions (capped at GTI) ===
        @Builder.Default private double totalDeductions = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Tax Computation — Part B TTI
    // Rules 117, 200-201, 141
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaxComputation {
        @Builder.Default private double grossTotalIncome = 0;
        @Builder.Default private double totalDeductions = 0;
        // B6 rounded to nearest 10
        @Builder.Default private double totalTaxableIncome = 0;

        private RegimeTaxBreakdown oldRegime;
        private RegimeTaxBreakdown newRegime;

        @Builder.Default private String selectedRegime = "new";
        @Builder.Default private double taxOnIncome = 0;
        @Builder.Default private double rebateU87A = 0;
        @Builder.Default private double taxAfterRebate = 0;
        @Builder.Default private double surcharge = 0;
        @Builder.Default private double cessAt4Pct = 0;
        @Builder.Default private double totalTaxLiability = 0;
        @Builder.Default private double reliefU89 = 0;
        @Builder.Default private double totalTaxAfterRelief = 0;

        @Builder.Default private double totalTaxesPaid = 0;
        @Builder.Default private double balanceTaxPayable = 0;
        @Builder.Default private double refundDue = 0;
        @Builder.Default private double interestPayable = 0;
        @Builder.Default private double totalTaxAndInterest = 0;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegimeTaxBreakdown {
        @Builder.Default private String regime = "old";
        @Builder.Default private double taxableIncome = 0;
        @Builder.Default private double taxOnIncome = 0;
        @Builder.Default private double rebateU87A = 0;
        @Builder.Default private double taxAfterRebate = 0;
        @Builder.Default private double surcharge = 0;
        @Builder.Default private double cessAt4Pct = 0;
        @Builder.Default private double totalTax = 0;
        @Builder.Default private double marginalRelief = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Schedule Taxes Paid
    // Rules 95-111, 193
    // ═══════════════════════════════════════════════════════════════
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScheduleTaxesPaid {
        @Builder.Default private double tdsOnSalary = 0;         // TDS1
        @Builder.Default private double tdsOtherThanSalary = 0;  // TDS2
        @Builder.Default private double tds3 = 0;                // TDS3
        @Builder.Default private double tcs = 0;
        @Builder.Default private double advanceTax = 0;
        @Builder.Default private double selfAssessmentTax = 0;
        // === Computed: Total [Rule 104] ===
        @Builder.Default private double totalTaxesPaid = 0;
    }
}
