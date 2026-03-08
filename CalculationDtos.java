package com.itr1.calculator.dto;

import com.itr1.calculator.model.ITR1Result;

/**
 * REST API DTOs for the calculation endpoint.
 */
public class CalculationDtos {

    /**
     * Request: POST /api/clients/{clientId}/prefill/{year}/calculate
     */
    public static class CalculationRequest {
        /** Raw prefill JSON as string (already stored in client_year_data.raw_prefill_json) */
        public String rawPrefillJson;

        /**
         * Optional: expected filing date yyyy-MM-dd.
         * Used to compute 234F fee. Defaults to today if null.
         */
        public String filingDate;

        /** Override: user-supplied deductions not in prefill (e.g., 80C investments) */
        public UserDeductionOverrides deductionOverrides;
    }

    /**
     * User-supplied deduction overrides — values from prefill are replaced
     * by these when non-null.
     */
    public static class UserDeductionOverrides {
        public Long section80C;
        public Long section80CCC;
        public Long section80CCD_Employee;
        public Long section80CCD_1B;
        public Long section80CCD_Employer;
        public Long section80D;
        public Long section80DD;
        public Long section80DDB;
        public Long section80E;
        public Long section80EE;
        public Long section80EEA;
        public Long section80EEB;
        public Long section80G;
        public Long section80GG;
        public Long section80GGA;
        public Long section80GGC;
        public Long section80TTA;
        public Long section80TTB;
        public Long section80U;
        public Long reliefUs89;

        // House property
        public String typeOfHP;
        public Long grossRentReceived;
        public Long taxPaidLocalAuth;
        public Long interestOnBorrowedCapital;
    }

    /**
     * Response: computed ITR-1 result + final submission JSON + validation messages.
     */
    public static class CalculationResponse {
        public Summary summary;
        public Taxes   taxes;
        public Refund  refund;
        public String  itr1Json;           // Final ITR-1 JSON for submission
        public Validation validation;

        public static class Summary {
            public String taxRegime;
            public long grossSalary;
            public long incomeFromSalary;
            public long totalIncomeOfHP;
            public long incomeFromOtherSources;
            public long grossTotalIncome;
            public long totalDeductions;
            public long totalIncome;
        }

        public static class Taxes {
            public long taxOnTotalIncome;
            public long rebate87A;
            public long taxAfterRebate;
            public long educationCess;
            public long grossTaxLiability;
            public long reliefUs89;
            public long netTaxLiability;
            public long interest234A;
            public long interest234B;
            public long interest234C;
            public long fee234F;
            public long totalTaxFeeInterest;
            public long totalTaxesPaid;
        }

        public static class Refund {
            public long refundDue;
            public long balTaxPayable;
        }

        public static class Validation {
            public boolean canUpload;             // false if any Category A errors
            public java.util.List<ITR1Result.ValidationMessage> errors;
            public java.util.List<ITR1Result.ValidationMessage> warnings;
            public java.util.List<ITR1Result.ValidationMessage> infos;
        }
    }

    /**
     * Maps ITR1Result → CalculationResponse.
     */
    public static CalculationResponse toResponse(ITR1Result result, String itr1Json) {
        CalculationResponse resp = new CalculationResponse();

        CalculationResponse.Summary s = new CalculationResponse.Summary();
        s.taxRegime              = result.taxRegime.name();
        s.grossSalary            = result.grossSalary;
        s.incomeFromSalary       = result.incomeFromSalary;
        s.totalIncomeOfHP        = result.totalIncomeOfHP;
        s.incomeFromOtherSources = result.netIncomeOthSrc;
        s.grossTotalIncome       = result.grossTotalIncome;
        s.totalDeductions        = result.totalChapVIADeductions;
        s.totalIncome            = result.totalIncome;
        resp.summary = s;

        CalculationResponse.Taxes t = new CalculationResponse.Taxes();
        t.taxOnTotalIncome   = result.totalTaxPayable;
        t.rebate87A          = result.rebate87A;
        t.taxAfterRebate     = result.taxAfterRebate;
        t.educationCess      = result.educationCess;
        t.grossTaxLiability  = result.grossTaxLiability;
        t.reliefUs89         = result.reliefUs89;
        t.netTaxLiability    = result.netTaxLiability;
        t.interest234A       = result.intrstPayUs234A;
        t.interest234B       = result.intrstPayUs234B;
        t.interest234C       = result.intrstPayUs234C;
        t.fee234F            = result.lateFilingFee234F;
        t.totalTaxFeeInterest = result.totalTaxFeeInterest;
        t.totalTaxesPaid     = result.totalTaxesPaid;
        resp.taxes = t;

        CalculationResponse.Refund r = new CalculationResponse.Refund();
        r.refundDue     = result.refundDue;
        r.balTaxPayable = result.balTaxPayable;
        resp.refund = r;

        resp.itr1Json = itr1Json;

        CalculationResponse.Validation v = new CalculationResponse.Validation();
        v.errors   = result.errors;
        v.warnings = result.warnings;
        v.infos    = result.infos;
        v.canUpload = (result.errors == null || result.errors.isEmpty());
        resp.validation = v;

        return resp;
    }
}
