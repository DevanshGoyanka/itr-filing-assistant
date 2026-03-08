package com.itr.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itr.model.ITR1Result;
import com.itr.model.PreFillData;
import com.itr.model.TaxRegime;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Builds the final ITR-1 JSON matching ITR-1_2025_Main_V1.2 schema structure.
 * This JSON can be submitted to the Income Tax e-filing portal.
 */
@Component
public class ITR1JsonBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String ASSESSMENT_YEAR = "2025-26";
    private static final String SCHEMA_VER = "Ver1.2";
    private static final String FORM_VER   = "Ver1.2";
    private static final String SW_CREATED_BY  = "SW00000001";
    private static final String JSON_CREATED_BY = "SW00000001";

    public String build(PreFillData prefill, ITR1Result result) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr  = root.putObject("ITR");
            ObjectNode itr1 = itr.putObject("ITR1");

            itr1.set("CreationInfo",           creationInfo());
            itr1.set("Form_ITR1",              formItr1());
            itr1.set("PersonalInfo",           personalInfo(prefill));
            itr1.set("FilingStatus",           filingStatus(prefill, result));
            itr1.set("ITR1_IncomeDeductions",  incomeDeductions(prefill, result));
            itr1.set("ITR1_TaxComputation",    taxComputation(result));
            itr1.set("TaxPaid",                taxPaid(result));
            itr1.set("Refund",                 refund(prefill, result));
            itr1.set("Verification",           verification(prefill));
            itr1.set("TDSonSalaries",          tdsOnSalaries(prefill));
            itr1.set("TDSonOthThanSals",       tdsOnOtherThanSalaries(prefill));
            itr1.set("TaxPayments",            taxPayments(prefill, result));

            if (result.taxRegime == TaxRegime.OLD_REGIME) {
                addDeductionSchedules(itr1, prefill, result);
            }

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build ITR-1 JSON: " + e.getMessage(), e);
        }
    }

    // ── CreationInfo ──────────────────────────────────────────────────────────

    private ObjectNode creationInfo() {
        ObjectNode n = mapper.createObjectNode();
        n.put("SWVersionNo",      "1.0");
        n.put("SWCreatedBy",      SW_CREATED_BY);
        n.put("JSONCreatedBy",    JSON_CREATED_BY);
        n.put("JSONCreationDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        n.put("IntermediaryCity", "Delhi");
        n.put("Digest",           "-");
        return n;
    }

    // ── Form_ITR1 ─────────────────────────────────────────────────────────────

    private ObjectNode formItr1() {
        ObjectNode n = mapper.createObjectNode();
        n.put("FormName",       "ITR-1");
        n.put("Description",    "For Individuals having income from Salaries, one house property, other sources (Interest etc.) and having total income upto Rs.50 lakh");
        n.put("AssessmentYear", ASSESSMENT_YEAR);
        n.put("SchemaVer",      SCHEMA_VER);
        n.put("FormVer",        FORM_VER);
        return n;
    }

    // ── PersonalInfo ──────────────────────────────────────────────────────────

    private ObjectNode personalInfo(PreFillData p) {
        ObjectNode n = mapper.createObjectNode();

        ObjectNode name = n.putObject("AssesseeName");
        name.put("FirstName",  p.firstName);
        name.put("MiddleName", p.middleName != null ? p.middleName : "");
        name.put("SurNameOrOrgName", p.lastName);

        n.put("PAN", p.pan);

        ObjectNode addr = n.putObject("Address");
        addr.put("ResidenceNo",         p.flatNo);
        addr.put("ResidenceName",       p.premisesName != null ? p.premisesName : "");
        addr.put("RoadOrStreet",        p.road);
        addr.put("LocalityOrArea",      p.area);
        addr.put("CityOrTownOrDistrict", p.city);
        addr.put("StateCode",           p.state);
        addr.put("PinCode",             p.pinCode);
        addr.put("CountryCode",         "91");
        addr.put("MobileNo",            p.mobile != null ? p.mobile : "");
        addr.put("EmailAddress",        p.email != null ? p.email : "");

        n.put("DOB",           p.dob);
        n.put("AadhaarCardNo", p.aadhaarCardNo != null ? p.aadhaarCardNo : "");
        n.put("EmployerCategory", p.employerCategoryCode != null ? p.employerCategoryCode : "OTH");

        return n;
    }

    // ── FilingStatus ──────────────────────────────────────────────────────────

    private ObjectNode filingStatus(PreFillData p, ITR1Result r) {
        ObjectNode n = mapper.createObjectNode();
        n.put("ReturnFileSec",     "11");  // 139(1) — original return
        n.put("OptOutNewTaxRegime", r.taxRegime == TaxRegime.OLD_REGIME ? "Y" : "N");
        n.put("SeventhProvisio139", "N");
        n.put("ItrFilingDueDate",  "2025-07-31");
        return n;
    }

    // ── ITR1_IncomeDeductions ─────────────────────────────────────────────────

    private ObjectNode incomeDeductions(PreFillData p, ITR1Result r) {
        ObjectNode n = mapper.createObjectNode();

        // Salary
        n.put("GrossSalary",          r.grossSalary);
        n.put("Salary",               r.salary17_1);
        n.put("PerquisitesValue",     r.perquisites17_2);
        n.put("ProfitsInSalary",      r.profitsInLieu17_3);
        n.put("AllwncExemptUs10",     r.allowancesExemptUs10);
        n.put("NetSalary",            r.netSalary);
        n.put("DeductionUs16",        r.deductionUs16);
        n.put("DeductionUs16ia",      r.stdDeductionUs16ia);
        n.put("EntertainmentAlw16ii", r.entertainmentAlw16ii);
        n.put("ProfessionalTaxUs16iii", r.professionalTaxUs16iii);
        n.put("IncomeFromSal",        r.incomeFromSalary);

        // House Property
        if (r.typeOfHP != null) {
            n.put("TypeOfHP",           r.typeOfHP);
            n.put("GrossRentReceived",  r.grossRentReceived);
            n.put("TaxPaidlocalAuth",   r.taxPaidLocalAuth);
            n.put("AnnualValue",        r.annualValue);
            n.put("StandardDeduction",  r.standardDeductionHP);
            n.put("InterestPayable",    r.interestPayable);
            n.put("ArrearsUnrealizedRentRcvd", r.arrearsUnrealizedRent);
            n.put("TotalIncomeOfHP",    r.totalIncomeOfHP);
        }

        // Other Sources
        n.put("IncomeOthSrc",         r.incomeOthSrc);

        ObjectNode othersInc = n.putObject("OthersInc");
        ArrayNode  othArr    = othersInc.putArray("OtherSourceDtls");
        addOtherSrcEntry(othArr, "SAV", r.savingsAccountInterest);
        addOtherSrcEntry(othArr, "DIV", r.dividendIncome);
        addOtherSrcEntry(othArr, "FD",  r.fdInterest);
        addOtherSrcEntry(othArr, "FAM", r.familyPension);
        addOtherSrcEntry(othArr, "OTH", r.otherIncome);

        n.put("DeductionUs57iia",     r.deductionUs57iia);

        // GTI
        n.put("GrossTotIncome",       r.grossTotalIncome);
        n.put("GrossTotIncomeIncLTCG112A", r.grossTotalIncome); // no LTCG in ITR-1

        // Chapter VI-A
        n.set("UsrDeductUndChapVIA",  chapViaUser(r));
        n.set("DeductUndChapVIA",     chapViaComputed(r));

        n.put("TotalIncome",          r.totalIncome);

        return n;
    }

    private void addOtherSrcEntry(ArrayNode arr, String code, long amt) {
        if (amt > 0) {
            ObjectNode e = arr.addObject();
            e.put("NatureDesc", code);
            e.put("OthAmount",  amt);
        }
    }

    private ObjectNode chapViaUser(ITR1Result r) {
        ObjectNode n = mapper.createObjectNode();
        n.put("Section80C",              r.sec80C);
        n.put("Section80CCC",            r.sec80CCC);
        n.put("Section80CCDEmployeeOrSE", r.sec80CCD_1);
        n.put("Section80CCD1B",          r.sec80CCD_1B);
        n.put("Section80CCDEmployer",    r.sec80CCD_2);
        n.put("Section80D",              r.sec80D);
        n.put("Section80DD",             r.sec80DD);
        n.put("Section80E",              r.sec80E);
        n.put("Section80EE",             r.sec80EE);
        n.put("Section80EEA",            r.sec80EEA);
        n.put("Section80EEB",            r.sec80EEB);
        n.put("Section80G",              r.sec80G);
        n.put("Section80GG",             r.sec80GG);
        n.put("Section80GGA",            r.sec80GGA);
        n.put("Section80GGC",            r.sec80GGC);
        n.put("Section80TTA",            r.sec80TTA);
        n.put("Section80TTB",            r.sec80TTB);
        n.put("Section80U",              r.sec80U);
        n.put("AnyOthSec80CCH",          r.sec80CCH);
        n.put("TotalChapVIADeductions",  r.totalChapVIADeductions);
        return n;
    }

    private ObjectNode chapViaComputed(ITR1Result r) {
        // Same values — CPC will validate
        return chapViaUser(r);
    }

    // ── ITR1_TaxComputation ───────────────────────────────────────────────────

    private ObjectNode taxComputation(ITR1Result r) {
        ObjectNode n = mapper.createObjectNode();
        n.put("TotalTaxPayable",    r.totalTaxPayable);
        n.put("Rebate87A",          r.rebate87A);
        n.put("TaxPayableOnRebate", r.taxAfterRebate);
        n.put("EducationCess",      r.educationCess);
        n.put("GrossTaxLiability",  r.grossTaxLiability);
        n.put("Section89",          r.reliefUs89);
        n.put("NetTaxLiability",    r.netTaxLiability);
        n.put("TotalIntrstPay",     r.totalInterestFee);

        ObjectNode intrstPay = n.putObject("IntrstPay");
        intrstPay.put("IntrstPayUs234A", r.intrstPayUs234A);
        intrstPay.put("IntrstPayUs234B", r.intrstPayUs234B);
        intrstPay.put("IntrstPayUs234C", r.intrstPayUs234C);
        intrstPay.put("LateFilingFee234F", r.lateFilingFee234F);

        n.put("TotTaxPlusIntrstPay", r.totalTaxFeeInterest);
        return n;
    }

    // ── TaxPaid ───────────────────────────────────────────────────────────────

    private ObjectNode taxPaid(ITR1Result r) {
        ObjectNode n = mapper.createObjectNode();

        ObjectNode taxesPaid = n.putObject("TaxesPaid");
        taxesPaid.put("AdvanceTax",        r.advanceTaxTotal);
        taxesPaid.put("TDS",               r.tdsTotal);
        taxesPaid.put("TCS",               r.tcsTotal);
        taxesPaid.put("SelfAssessmentTax", r.selfAssessmentTaxTotal);
        taxesPaid.put("TotalTaxesPaid",    r.totalTaxesPaid);

        n.put("BalTaxPayable", r.balTaxPayable);
        return n;
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    private ObjectNode refund(PreFillData p, ITR1Result r) {
        ObjectNode n = mapper.createObjectNode();
        n.put("RefundDue", r.refundDue);
        if (p.aadhaarCardNo != null && !p.aadhaarCardNo.isBlank()) {
            // Bank account details would come from prefill bankAccountDtls
            ObjectNode bank = n.putObject("BankAccountDtls");
            bank.put("IFSCCode", "");      // populated from prefill in real use
            bank.put("BankName", "");
            bank.put("BankAccountNo", "");
            bank.put("AccountType", "SB");
        }
        return n;
    }

    // ── Verification ──────────────────────────────────────────────────────────

    private ObjectNode verification(PreFillData p) {
        ObjectNode n = mapper.createObjectNode();
        ObjectNode decl = n.putObject("Declaration");
        decl.put("AssesseeVerPAN",  p.pan);
        decl.put("AssesseeVerName", (p.firstName + " " + p.middleName + " " + p.lastName).trim());
        decl.put("FatherName",      "");
        decl.put("Capacity",        "S");
        n.put("Place", p.city != null ? p.city : "");
        n.put("Date",  LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return n;
    }

    // ── TDS on Salaries ───────────────────────────────────────────────────────

    private ObjectNode tdsOnSalaries(PreFillData p) {
        ObjectNode n = mapper.createObjectNode();
        ArrayNode  arr = n.putArray("TDSonSalary");
        if (p.tdsOnSalary != null) {
            for (PreFillData.TdsEntry e : p.tdsOnSalary) {
                ObjectNode entry = arr.addObject();
                ObjectNode emp   = entry.putObject("EmployerOrDeductorOrCollectDetl");
                emp.put("TAN",                             e.tan);
                emp.put("EmployerOrDeductorOrCollecterName", e.deductorName);
                entry.put("IncChrgSal",   e.grossAmount);
                entry.put("TotalTDSSal",  e.taxDeducted);
                entry.put("TDSClaimed",   e.taxClaimed);
            }
        }
        return n;
    }

    // ── TDS on Other Than Salaries ────────────────────────────────────────────

    private ObjectNode tdsOnOtherThanSalaries(PreFillData p) {
        ObjectNode n = mapper.createObjectNode();
        ArrayNode  arr = n.putArray("TDSonOthThanSal");
        if (p.tdsOnOtherIncome != null) {
            for (PreFillData.TdsEntry e : p.tdsOnOtherIncome) {
                ObjectNode entry = arr.addObject();
                ObjectNode ded   = entry.putObject("EmployerOrDeductorOrCollectDetl");
                ded.put("TAN",                             e.tan);
                ded.put("EmployerOrDeductorOrCollecterName", e.deductorName);
                entry.put("GrossAmt",  e.grossAmount);
                entry.put("TotalTDS",  e.taxDeducted);
                entry.put("TDSClaimed", e.taxClaimed);
            }
        }
        return n;
    }

    // ── Tax Payments (Advance Tax / SAT) ─────────────────────────────────────

    private ObjectNode taxPayments(PreFillData p, ITR1Result r) {
        ObjectNode n   = mapper.createObjectNode();
        ArrayNode  arr = n.putArray("TaxPayment");
        if (p.taxPayments != null) {
            for (PreFillData.TaxPaymentEntry e : p.taxPayments) {
                ObjectNode entry = arr.addObject();
                entry.put("BSRCode",        e.bsrCode);
                entry.put("DateDep",        e.dateOfDeposit);
                entry.put("SrlNoOfChaln",   e.challanNo);
                entry.put("Amt",            e.amount);
            }
        }
        return n;
    }

    // ── Deduction Schedules (old regime) ─────────────────────────────────────

    private void addDeductionSchedules(ObjectNode itr1, PreFillData p, ITR1Result r) {
        if (r.sec80E > 0) {
            ObjectNode sch80E = itr1.putObject("Schedule80E");
            sch80E.put("TotalInterest80E", r.sec80E);
        }
        if (r.sec80U > 0) {
            ObjectNode sch80U = itr1.putObject("Schedule80U");
            sch80U.put("DeductionAmount", r.sec80U);
        }
        if (r.sec80D > 0 && p.sch80DDetail != null) {
            itr1.set("Schedule80D", schedule80D(p.sch80DDetail));
        }
    }

    private ObjectNode schedule80D(PreFillData.Sch80DDetail d) {
        ObjectNode n = mapper.createObjectNode();
        ObjectNode sec = n.putObject("Sec80DSelfFamSrCtznHealth");
        sec.put("SelfFamilyTotal",   d.selfFamilyTotal);
        sec.put("ParentsTotal",      d.parentsTotal);
        sec.put("EligibleAmount",    d.eligibleAmount);
        return n;
    }
}
