package com.itr.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.model.PreFillData;
import com.itr.model.PreFillData.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps raw prefill JSON (PreFillSchemaJSON_V6.5) to {@link PreFillData}.
 *
 * Prefill field paths resolved from the sample COVPC5929M JSON.
 */
@Component
public class PrefillMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PreFillData map(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            PreFillData data = new PreFillData();

            mapPersonalInfo(root, data);
            mapFilingStatus(root, data);
            mapSalary(root, data);
            mapOtherIncome(root, data);
            mapTdsOnSalary(root, data);
            mapTdsOnOtherIncome(root, data);
            mapTaxPayments(root, data);
            mapDeductions(root, data);

            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse prefill JSON: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Personal Info
    // ──────────────────────────────────────────────────────────────────────────

    private void mapPersonalInfo(JsonNode root, PreFillData data) {
        JsonNode pi = root.path("personalInfo");

        data.pan = pi.path("pan").asText("");
        data.dob = pi.path("dob").asText("");
        data.residentialStatus = pi.path("filingStatus").path("residentialStatus").asText("RES");

        JsonNode name = pi.path("assesseeName");
        data.firstName  = name.path("firstName").asText("");
        data.middleName = name.path("middleName").asText("");
        data.lastName   = name.path("surNameOrOrgName").asText("");

        data.aadhaarCardNo = pi.path("aadhaarCardNo").asText("");
        data.email         = pi.path("address").path("emailAddress").asText("");
        data.mobile        = pi.path("address").path("mobileNo").asText("");

        JsonNode addr = pi.path("address");
        data.flatNo        = addr.path("residenceNo").asText("");
        data.premisesName  = addr.path("residenceName").asText("");
        data.road          = addr.path("roadOrStreet").asText("");
        data.area          = addr.path("localityOrArea").asText("");
        data.city          = addr.path("cityOrTownOrDistrict").asText("");
        data.state         = addr.path("stateCode").asText("");
        data.pinCode       = addr.path("pinCode").asText("");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Filing Status
    // ──────────────────────────────────────────────────────────────────────────

    private void mapFilingStatus(JsonNode root, PreFillData data) {
        JsonNode fs = root.path("filingStatus");
        // 2 = new regime opted (default), 1 = old regime opted
        data.optingNewTaxRegime = fs.path("OptingNewTaxRegimeForm10IF").asInt(2);
        
        // Check form24q for pensioner flag
        String pensionerFlag = root.path("form24q").path("PensionerFlag").asText("N");
        if ("Y".equalsIgnoreCase(pensionerFlag)) {
            data.employerCategoryCode = "PEN";
        } else {
            // Default to "OTH" (Other) - can be overridden if employer details indicate govt
            data.employerCategoryCode = "OTH";
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Salary (from form24q — most accurate source, falls back to insights)
    // ──────────────────────────────────────────────────────────────────────────

    private void mapSalary(JsonNode root, PreFillData data) {
        // Primary: form24q salary details
        JsonNode form24q = root.path("form24q");
        JsonNode incDed  = form24q.path("incomeDeductions");

        if (!incDed.isMissingNode() && !incDed.isNull()) {
            data.salary17_1       = longVal(incDed, "salary");
            data.perquisites17_2  = longVal(incDed, "perquisitesValue");
            data.profitsInLieu17_3 = longVal(incDed, "profitsInSalary");
            data.professionalTaxUs16iii = longVal(incDed, "professionalTaxUs16Iii");
            data.totalAllwncExemptUs10 = longVal(incDed, "TotalAllwncExemptUs10");
        }

        // Also try form24q.salaries.salary array (alternative structure)
        JsonNode sals = form24q.path("salaries").path("salary");
        if (sals.isArray() && sals.size() > 0) {
            JsonNode sal = sals.get(0);
            JsonNode salDetails = sal.path("salarys");
            
            // If incomeDeductions was empty, use salary array data
            if (data.salary17_1 == 0) {
                data.salary17_1 = longVal(salDetails, "salary");
            }
            if (data.perquisites17_2 == 0) {
                data.perquisites17_2 = longVal(salDetails, "valueOfPerquisites");
            }
            if (data.profitsInLieu17_3 == 0) {
                data.profitsInLieu17_3 = longVal(salDetails, "profitsinLieuOfSalary");
            }
            
            data.employerName = sal.path("nameOfEmployer").asText("");
            data.employerTAN  = sal.path("tanOfEmployer").asText("");
        }

        // Fallback: insights.cumulativeSalary
        if (data.salary17_1 == 0) {
            JsonNode cs = root.path("insights").path("cumulativeSalary");
            data.salary17_1      = longVal(cs, "salary");
            data.perquisites17_2 = longVal(cs, "perquisitesValue");
            data.profitsInLieu17_3 = longVal(cs, "profitsInSalary");
        }

        // TDS on salary from form26as
        JsonNode tdsSalArr = root.path("form26as").path("tdsOnSalaries").path("tdsOnSalary");
        if (tdsSalArr.isArray() && tdsSalArr.size() > 0) {
            data.tdsSalary = longVal(tdsSalArr.get(0), "totalTDSSal");
            if (data.employerTAN == null || data.employerTAN.isEmpty()) {
                data.employerTAN  = tdsSalArr.get(0).path("employerOrDeductorOrCollectDetl").path("tan").asText("");
                data.employerName = tdsSalArr.get(0).path("employerOrDeductorOrCollectDetl")
                        .path("employerOrDeductorOrCollecterName").asText("");
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Other Income
    // ──────────────────────────────────────────────────────────────────────────

    private void mapOtherIncome(JsonNode root, PreFillData data) {
        // From insights
        JsonNode insights = root.path("insights");
        data.savingsAccountInterest = longVal(insights, "intrstFrmSavingBank");

        JsonNode schedOS = insights.path("scheduleOS").path("incOthThanOwnRaceHorse");
        data.dividendIncome = longVal(schedOS, "dividendGross");

        // From incomeDeductionsOthersInc array (insights)
        JsonNode othInc = insights.path("incomeDeductionsOthersInc");
        if (othInc.isArray()) {
            for (JsonNode item : othInc) {
                String desc = item.path("othSrcNatureDesc").asText("");
                long amt    = longVal(item, "othSrcOthAmount");
                switch (desc.toUpperCase()) {
                    case "SAV": data.savingsAccountInterest = amt; break;
                    case "DIV": data.dividendIncome        = amt; break;
                    case "FD":  data.fdInterest            = amt; break;
                    case "FAM": data.familyPension         = amt; break;
                    default:    data.otherIncome          += amt; break;
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TDS on Salary
    // ──────────────────────────────────────────────────────────────────────────

    private void mapTdsOnSalary(JsonNode root, PreFillData data) {
        data.tdsOnSalary = new ArrayList<>();
        JsonNode arr = root.path("form26as").path("tdsOnSalaries").path("tdsOnSalary");
        if (arr.isArray()) {
            for (JsonNode node : arr) {
                TdsEntry e = new TdsEntry();
                e.tan          = node.path("employerOrDeductorOrCollectDetl").path("tan").asText("");
                e.deductorName = node.path("employerOrDeductorOrCollectDetl")
                                     .path("employerOrDeductorOrCollecterName").asText("");
                e.grossAmount  = longVal(node, "incChrgSal");
                e.taxDeducted  = longVal(node, "totalTDSSal");
                e.taxClaimed   = e.taxDeducted;
                data.tdsOnSalary.add(e);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TDS on Other Than Salary
    // ──────────────────────────────────────────────────────────────────────────

    private void mapTdsOnOtherIncome(JsonNode root, PreFillData data) {
        data.tdsOnOtherIncome = new ArrayList<>();
        JsonNode arr = root.path("form26as").path("tdsOnOthThanSals").path("tdSonOthThanSal");
        if (arr.isArray()) {
            for (JsonNode node : arr) {
                TdsEntry e = new TdsEntry();
                e.tan          = node.path("employerOrDeductorOrCollectDetl").path("tan").asText("");
                e.deductorName = node.path("employerOrDeductorOrCollectDetl")
                                     .path("employerOrDeductorOrCollecterName").asText("");
                e.grossAmount  = longVal(node, "grossAmt");
                e.taxDeducted  = longVal(node, "totalTDS");
                e.taxClaimed   = longVal(node, "tdsClaimed");
                data.tdsOnOtherIncome.add(e);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tax Payments (Advance Tax / Self-Assessment)
    // ──────────────────────────────────────────────────────────────────────────

    private void mapTaxPayments(JsonNode root, PreFillData data) {
        data.taxPayments = new ArrayList<>();
        JsonNode arr = root.path("form26as").path("taxPayments").path("taxPayment");
        if (arr.isArray()) {
            for (JsonNode node : arr) {
                TaxPaymentEntry e = new TaxPaymentEntry();
                e.bsrCode     = node.path("bsrCode").asText("");
                e.challanNo   = node.path("srlNoOfChaln").asText("");
                e.dateOfDeposit = node.path("dateDep").asText("");
                e.amount      = longVal(node, "amt");
                data.taxPayments.add(e);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Deductions u/s Chapter VI-A
    // ──────────────────────────────────────────────────────────────────────────

    private void mapDeductions(JsonNode root, PreFillData data) {
        JsonNode dedType = root.path("form24q").path("usrDeductUndChapVIAType");

        data.section80C              = longVal(dedType, "section80C");
        data.section80CCC            = longVal(dedType, "section80CCC");
        data.section80CCD_Employee   = longVal(dedType, "section80CCDEmployeeOrSE");
        data.section80CCD_1B         = longVal(dedType, "section80CCD1B");
        data.section80CCD_Employer   = longVal(dedType, "section80CCDEmployer");
        data.section80TTA            = longVal(dedType, "section80TTA");
        data.section80E              = longVal(dedType, "section80E");
        data.section80D              = longVal(dedType, "section80D");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private long longVal(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return 0L;
        JsonNode f = node.path(field);
        if (f.isMissingNode() || f.isNull()) return 0L;
        return f.asLong(0L);
    }
}
