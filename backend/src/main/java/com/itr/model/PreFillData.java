package com.itr.model;

import java.util.List;

/**
 * Java representation of key fields from PreFillSchemaJSON_V6.5
 * populated by the PrefillMapper from raw portal JSON.
 */
public class PreFillData {

    // ── Personal Info ─────────────────────────────────────────────────────────
    public String pan;
    public String firstName;
    public String middleName;
    public String lastName;
    public String dob;              // yyyy-MM-dd
    public String aadhaarCardNo;
    public String email;
    public String mobile;
    public String residentialStatus; // "RES", "NOR", "NRI"

    // ── Address ───────────────────────────────────────────────────────────────
    public String flatNo;
    public String premisesName;
    public String road;
    public String area;
    public String city;
    public String state;
    public String pinCode;
    public String country;

    // ── Filing Status ─────────────────────────────────────────────────────────
    /**
     * 2 = new regime, 1 = old regime
     * from filingStatus.OptingNewTaxRegimeForm10IF
     */
    public int optingNewTaxRegime;   // 2 = new, 1 = old

    // ── Salary (from form24q / insights) ─────────────────────────────────────
    /** Salary as per 17(1) — from employer TDS data */
    public long salary17_1;
    /** Value of perquisites as per 17(2) */
    public long perquisites17_2;
    /** Profits in lieu of salary as per 17(3) */
    public long profitsInLieu17_3;
    /** Employer name */
    public String employerName;
    /** Employer TAN */
    public String employerTAN;
    /** Employer category code */
    public String employerCategoryCode;
    /** Total TDS deducted on salary */
    public long tdsSalary;
    /** Professional tax u/s 16(iii) */
    public long professionalTaxUs16iii;

    // ── Allowances exempt u/s 10 (user-supplied) ─────────────────────────────
    public long hra10_13A;
    public long lta10_5;
    public long otherExempt10_14_i;
    public long otherExempt10_14_ii;
    public long gratuity10_10;
    public long leaveEncashment10_10AA;
    public long vrsAmount10_10C;
    public long totalAllwncExemptUs10;  // sum of all above

    // ── House Property ────────────────────────────────────────────────────────
    /** "S" = Self-Occupied, "L" = Let-Out, "D" = Deemed Let-Out */
    public String typeOfHP;
    public long grossRentReceived;
    public long taxPaidLocalAuth;
    public long interestOnBorrowedCapital;
    public long arrearsUnrealizedRentRcvd;

    // ── Income from Other Sources ─────────────────────────────────────────────
    public long savingsAccountInterest;   // Interest from SB account
    public long fdInterest;               // FD / bank deposit interest
    public long dividendIncome;           // Dividend from shares/MF
    public long familyPension;            // Family pension received
    public long interestFromITRefund;
    public long otherIncome;              // Any other income not classified

    // ── TDS on other-than-salary ──────────────────────────────────────────────
    public List<TdsEntry> tdsOnOtherIncome;

    // ── TDS on salary (from 26AS/Form 24Q) ────────────────────────────────────
    public List<TdsEntry> tdsOnSalary;

    // ── Tax Payments (Advance Tax / SAT) ─────────────────────────────────────
    public List<TaxPaymentEntry> taxPayments;

    // ── TCS entries ───────────────────────────────────────────────────────────
    public List<TcsEntry> tcsEntries;

    // ── Deductions Chapter VI-A ───────────────────────────────────────────────
    public long section80C;
    public long section80CCC;
    public long section80CCD_Employee;  // 80CCD(1) — employee contribution
    public long section80CCD_1B;        // 80CCD(1B) — additional NPS
    public long section80CCD_Employer;  // 80CCD(2) — employer NPS contribution
    public String pranNumber;

    public long section80D;
    public long section80DD;
    public String section80DDB_category; // "1" or "2"
    public String section80DDB_disease;
    public long section80DDB;
    public long section80E;             // Education loan interest
    public long section80EE;            // First home loan interest
    public long section80EEA;           // Affordable housing interest
    public long section80EEB;           // EV loan interest
    public long section80G;
    public long section80GG;            // House rent if no HRA
    public long section80GGA;
    public long section80GGC;
    public long section80TTA;           // Savings interest (< 60 yrs)
    public long section80TTB;           // Interest income (>= 60 yrs)
    public long section80U;             // Self disability
    public long section80CCH;           // Agnipath scheme

    // ── Schedule 80D detail ───────────────────────────────────────────────────
    public Sch80DDetail sch80DDetail;

    // ── Relief ───────────────────────────────────────────────────────────────
    public long reliefUs89;             // Relief for arrears

    // ── Exempt income (agriculture / others) ─────────────────────────────────
    public long agricultureIncome;

    // ── Inner classes ─────────────────────────────────────────────────────────

    public static class TdsEntry {
        public String tan;
        public String deductorName;
        public long grossAmount;
        public long taxDeducted;
        public long taxClaimed;
        public String year;
    }

    public static class TaxPaymentEntry {
        public String bsrCode;
        public String challanNo;
        public String dateOfDeposit;   // yyyy-MM-dd
        public long amount;
    }

    public static class TcsEntry {
        public String tan;
        public String collectorName;
        public long taxCollected;
        public long taxClaimed;
        public String year;
    }

    public static class Sch80DDetail {
        // Self & family (< 60)
        public long selfFamilyHealthInsurance;
        public long selfFamilyPreventiveCheckup;
        public long selfFamilyTotal;          // max 25000

        // Self & family (>= 60 senior citizen)
        public long selfFamilySrHealthInsurance;
        public long selfFamilySrPreventiveCheckup;
        public long selfFamilySrMedicalExp;
        public long selfFamilySrTotal;        // max 50000

        // Parents (< 60)
        public long parentsHealthInsurance;
        public long parentsPreventiveCheckup;
        public long parentsTotal;             // max 25000

        // Parents (>= 60 senior citizen)
        public long parentsSrHealthInsurance;
        public long parentsSrPreventiveCheckup;
        public long parentsSrMedicalExp;
        public long parentsSrTotal;           // max 50000

        // Eligible deduction (max 1,00,000)
        public long eligibleAmount;
    }
}
