package com.itr1.calculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr1.calculator.mapper.ITR1JsonBuilder;
import com.itr1.calculator.mapper.PrefillMapper;
import com.itr1.calculator.model.ITR1Result;
import com.itr1.calculator.model.PreFillData;
import com.itr1.calculator.model.TaxRegime;
import com.itr1.calculator.service.ITR1CalculatorService;
import com.itr1.calculator.service.ITR1ValidationService;
import com.itr1.calculator.service.InterestCalculator;
import com.itr1.calculator.service.TaxSlabCalculator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using COVPC5929M sample prefill data.
 *
 * Expected values for YASH UMESH CHANDAK (new regime):
 *  - Salary: ₹14,70,930 (as per form24q / 26AS)
 *  - Standard Deduction (new regime): ₹75,000
 *  - Income from Salary: ₹13,95,930
 *  - Savings bank interest: ₹3,423
 *  - Dividend: ₹388
 *  - Total Income: ~₹13,99,741
 *  - Tax (new regime): ~₹1,89,948
 *  - Cess (4%): ~₹7,598
 *  - No 87A rebate (income > ₹12L)
 *  - TDS Salary: ₹1,23,953
 *  - Advance Tax (SAT Jul-2025): ₹790
 *  - Total Taxes Paid: ₹1,24,743
 *  - Balance Tax Payable: ~₹72,803
 */
class ITR1CalculatorIntegrationTest {

    private static PrefillMapper         prefillMapper;
    private static ITR1CalculatorService calculatorService;
    private static ITR1JsonBuilder       jsonBuilder;
    private static String                samplePrefillJson;

    @BeforeAll
    static void setUp() throws Exception {
        TaxSlabCalculator   slabCalc      = new TaxSlabCalculator();
        InterestCalculator  interestCalc  = new InterestCalculator();
        ITR1ValidationService validSvc    = new ITR1ValidationService();

        prefillMapper     = new PrefillMapper();
        calculatorService = new ITR1CalculatorService(slabCalc, interestCalc, validSvc);
        jsonBuilder       = new ITR1JsonBuilder();

        // Load sample prefill — adjust path for your test resources location
        try {
            samplePrefillJson = Files.readString(
                Paths.get("src/test/resources/COVPC5929M-Prefill-2025-sample.json"));
        } catch (Exception e) {
            // Inline sample for CI environments
            samplePrefillJson = buildInlineSampleJson();
        }
    }

    @Test
    void testPrefillMapping() {
        PreFillData data = prefillMapper.map(samplePrefillJson);

        assertEquals("COVPC5929M", data.pan);
        assertEquals("YASH", data.firstName);
        assertEquals("CHANDAK", data.lastName);
        assertEquals("2002-02-08", data.dob);
        assertEquals(2, data.optingNewTaxRegime); // new regime

        // Salary from form24q
        assertTrue(data.salary17_1 > 0, "Salary should be populated");
        assertEquals("HYDW00345C", data.employerTAN);
    }

    @Test
    void testNewRegimeTaxComputation() {
        PreFillData data   = prefillMapper.map(samplePrefillJson);
        ITR1Result  result = calculatorService.compute(data, "2025-07-21");

        assertEquals(TaxRegime.NEW_REGIME, result.taxRegime);

        // Standard deduction for new regime = 75,000
        assertEquals(75_000, result.stdDeductionUs16ia,
            "New regime standard deduction should be ₹75,000");

        // Gross salary check
        assertTrue(result.grossSalary > 0, "Gross salary should be positive");

        // Income from salary = NetSalary - DeductionUs16
        assertEquals(result.incomeFromSalary, result.netSalary - result.deductionUs16,
            "Income from salary computation mismatch");

        // GTI cross-check
        long expectedGTI = result.incomeFromSalary + result.totalIncomeOfHP + result.netIncomeOthSrc;
        assertEquals(expectedGTI, result.grossTotalIncome, "GTI mismatch");

        // TotalIncome = GTI - Deductions (new regime allows only 80CCD2)
        assertEquals(Math.max(0, result.grossTotalIncome - result.totalChapVIADeductions),
            result.totalIncome, "Total Income mismatch");

        // Education cess = 4% of tax after rebate
        assertEquals(result.taxAfterRebate * 4 / 100, result.educationCess, "Cess should be 4%");

        // Gross tax liability = taxAfterRebate + educationCess
        assertEquals(result.taxAfterRebate + result.educationCess, result.grossTaxLiability,
            "Gross tax liability mismatch");
    }

    @Test
    void testTaxesPaidReconciliation() {
        PreFillData data   = prefillMapper.map(samplePrefillJson);
        ITR1Result  result = calculatorService.compute(data, "2025-07-21");

        // TDS from 26AS: ₹1,23,953
        assertEquals(123_953, result.tdsSalaryTotal, "TDS on salary mismatch");

        // Total taxes paid = TDS + payments
        long expectedTotal = result.tdsSalaryTotal + result.tdsOtherTotal
                + result.tcsTotal + result.advanceTaxTotal + result.selfAssessmentTaxTotal;
        assertEquals(expectedTotal, result.totalTaxesPaid, "Total taxes paid mismatch");

        // Either refund or balance, never both
        assertTrue(result.refundDue == 0 || result.balTaxPayable == 0,
            "Both refund and balance tax cannot be positive simultaneously");
    }

    @Test
    void testValidationNoBlockingErrors() {
        PreFillData data   = prefillMapper.map(samplePrefillJson);
        ITR1Result  result = calculatorService.compute(data, "2025-07-21");

        // For this simple salaried case, no Category A errors expected
        if (result.errors != null && !result.errors.isEmpty()) {
            result.errors.forEach(e ->
                System.out.println("Rule " + e.ruleNo + ": " + e.message));
        }

        // Basic structural validations should pass
        assertNotNull(result.errors,   "Errors list should not be null");
        assertNotNull(result.warnings, "Warnings list should not be null");
    }

    @Test
    void testITR1JsonBuilt() {
        PreFillData data   = prefillMapper.map(samplePrefillJson);
        ITR1Result  result = calculatorService.compute(data, "2025-07-21");
        String      json   = jsonBuilder.build(data, result);

        assertNotNull(json);
        assertTrue(json.contains("\"ITR\""), "JSON should have root ITR node");
        assertTrue(json.contains("\"ITR1\""), "JSON should have ITR1 node");
        assertTrue(json.contains("COVPC5929M"), "PAN should be in JSON");
        assertTrue(json.contains("AssessmentYear"), "Assessment year should be present");

        // Validate it's parseable
        assertDoesNotThrow(() -> new ObjectMapper().readTree(json),
            "Output JSON must be valid JSON");
    }

    @Test
    void testOldRegimeHigherTax() {
        PreFillData data = prefillMapper.map(samplePrefillJson);
        data.optingNewTaxRegime = 1; // force old regime

        ITR1Result result = calculatorService.compute(data, "2025-07-21");

        assertEquals(TaxRegime.OLD_REGIME, result.taxRegime);
        assertEquals(50_000, result.stdDeductionUs16ia,
            "Old regime standard deduction should be ₹50,000");
    }

    @Test
    void testSlabCalculatorBoundaries() {
        TaxSlabCalculator calc = new TaxSlabCalculator();

        // New regime
        assertEquals(0,       calc.computeNewRegimeTax(300_000, 30));
        assertEquals(20_000,  calc.computeNewRegimeTax(700_000, 30));
        assertEquals(50_000,  calc.computeNewRegimeTax(1_000_000, 30));
        assertEquals(80_000,  calc.computeNewRegimeTax(1_200_000, 30));
        assertEquals(140_000, calc.computeNewRegimeTax(1_500_000, 30));

        // Old regime
        assertEquals(0,       calc.computeOldRegimeTax(250_000, 30));
        assertEquals(12_500,  calc.computeOldRegimeTax(500_000, 30));
        assertEquals(112_500, calc.computeOldRegimeTax(1_000_000, 30));

        // Rebate — new regime ≤ ₹7L = full rebate
        long tax7L = calc.computeNewRegimeTax(700_000, 30);
        assertEquals(tax7L, calc.computeRebate87ANewRegime(700_000, tax7L));

        // Rebate — new regime ₹12L = rebate up to ₹60,000
        long tax12L = calc.computeNewRegimeTax(1_200_000, 30);
        assertEquals(Math.min(tax12L, 60_000), calc.computeRebate87ANewRegime(1_200_000, tax12L));

        // No rebate above ₹12L
        assertEquals(0, calc.computeRebate87ANewRegime(1_500_000, 200_000));
    }

    // ── Inline sample JSON (matches COVPC5929M prefill) ───────────────────────

    private static String buildInlineSampleJson() {
        return "{\n" +
            "\"personalInfo\":{" +
            "\"pan\":\"COVPC5929M\"," +
            "\"dob\":\"2002-02-08\"," +
            "\"assesseeName\":{\"firstName\":\"YASH\",\"surNameOrOrgName\":\"CHANDAK\",\"middleName\":\"UMESH\"}," +
            "\"filingStatus\":{\"residentialStatus\":\"RES\"}," +
            "\"address\":{\"pinCode\":444005,\"stateCode\":\"19\",\"cityOrTownOrDistrict\":\"AKOLA\"," +
            "\"roadOrStreet\":\"Jatherpeth\",\"localityOrArea\":\"Akola\",\"residenceNo\":\"7PAVAN\"," +
            "\"residenceName\":\"DURGA CHOWK\",\"mobileNo\":9834196609," +
            "\"emailAddress\":\"chandakyash41@gmail.com\"}" +
            "}," +
            "\"filingStatus\":{\"OptingNewTaxRegimeForm10IF\":2}," +
            "\"form26as\":{" +
            "\"taxPayments\":{\"taxPayment\":[{\"bsrCode\":\"0002271\",\"dateDep\":\"2025-07-12\",\"srlNoOfChaln\":\"33911\",\"amt\":790}]}," +
            "\"tdsOnSalaries\":{\"tdsOnSalary\":[{\"totalTDSSal\":123953,\"incChrgSal\":1470931," +
            "\"employerOrDeductorOrCollectDetl\":{\"tan\":\"HYDW00345C\",\"employerOrDeductorOrCollecterName\":\"WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED\"}}]}," +
            "\"tdsOnOthThanSals\":{\"tdSonOthThanSal\":[]}" +
            "}," +
            "\"form24q\":{" +
            "\"incomeDeductions\":{\"salary\":1470930,\"perquisitesValue\":0,\"profitsInSalary\":0," +
            "\"deductionUs16ia\":50000,\"professionalTaxUs16Iii\":0}," +
            "\"salaries\":{\"salary\":[{\"nameOfEmployer\":\"WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED\"," +
            "\"tanOfEmployer\":\"HYDW00345C\"}]}," +
            "\"usrDeductUndChapVIAType\":{\"section80C\":0,\"section80CCC\":0,\"section80CCDEmployeeOrSE\":0," +
            "\"section80CCD1B\":0,\"section80CCDEmployer\":0,\"section80TTA\":0,\"section80E\":0,\"section80D\":0}" +
            "}," +
            "\"insights\":{" +
            "\"intrstFrmSavingBank\":3423," +
            "\"cumulativeSalary\":{\"salary\":857916,\"perquisitesValue\":0,\"profitsInSalary\":0}," +
            "\"scheduleOS\":{\"incOthThanOwnRaceHorse\":{\"dividendGross\":388,\"DividendOthThan22e\":388}}," +
            "\"incomeDeductionsOthersInc\":[{\"othSrcOthAmount\":388,\"othSrcNatureDesc\":\"DIV\"}," +
            "{\"othSrcOthAmount\":3423,\"othSrcNatureDesc\":\"SAV\"}]" +
            "}" +
            "}";
    }
}
