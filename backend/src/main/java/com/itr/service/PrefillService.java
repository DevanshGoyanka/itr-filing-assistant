package com.itr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itr.dto.PrefillResponse;
import com.itr.entity.Client;
import com.itr.entity.ClientYearData;
import com.itr.exception.BusinessLogicException;
import com.itr.exception.ConflictException;
import com.itr.exception.ResourceNotFoundException;
import com.itr.repository.ClientYearDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrefillService {

    private final ClientYearDataRepository yearDataRepository;
    private final ClientService clientService;
    private final ObjectMapper objectMapper;

    private static final Pattern AY_PATTERN = Pattern.compile("^AY\\d{4}-\\d{2}$");

    @Transactional
    public PrefillResponse uploadPrefill(Long clientId, String year, String jsonContent, Long userId) {
        // 1. Validate assessment year format
        validateAssessmentYear(year);

        // 2. Get and authorize client
        Client client = clientService.getClientEntity(clientId, userId);

        // 3. Parse and validate JSON
        JsonNode prefillJson = parseJson(jsonContent);

        // 4. Extract PAN and match
        String prefillPan = extractPan(prefillJson);
        if (prefillPan != null && !prefillPan.equals(client.getPan())) {
            throw new BusinessLogicException(
                    "PAN mismatch: Client PAN is " + client.getPan() +
                    " but prefill contains " + prefillPan);
        }

        // 5. Check for existing year data
        ClientYearData yearData = yearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElse(null);

        if (yearData != null) {
            // Update existing record
            yearData.setRawPrefillJson(jsonContent);
            yearData.setComputedItr1Json(mapToItr1(prefillJson, client));
            yearData.setItrType(detectItrType(prefillJson));
            yearData.setStatus("draft");
        } else {
            // Create new record
            yearData = ClientYearData.builder()
                    .client(client)
                    .assessmentYear(year)
                    .rawPrefillJson(jsonContent)
                    .computedItr1Json(mapToItr1(prefillJson, client))
                    .itrType(detectItrType(prefillJson))
                    .status("draft")
                    .build();
        }

        yearData = yearDataRepository.save(yearData);

        // 6. Build response with income summary
        return buildPrefillResponse(client, yearData, prefillJson);
    }

    @Transactional(readOnly = true)
    public PrefillResponse getPrefillData(Long clientId, String year, Long userId) {
        Client client = clientService.getClientEntity(clientId, userId);
        ClientYearData yearData = yearDataRepository
                .findByClientIdAndAssessmentYear(clientId, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No prefill data found for " + year));

        JsonNode prefillJson;
        try {
            prefillJson = objectMapper.readTree(yearData.getRawPrefillJson());
        } catch (JsonProcessingException e) {
            prefillJson = objectMapper.createObjectNode();
        }

        return buildPrefillResponse(client, yearData, prefillJson);
    }

    private void validateAssessmentYear(String year) {
        if (!AY_PATTERN.matcher(year).matches()) {
            throw new IllegalArgumentException("Invalid assessment year format. Must be like AY2025-26");
        }
        int startYear = Integer.parseInt(year.substring(2, 6));
        int endYear = Integer.parseInt(year.substring(7, 9));
        if ((startYear % 100) + 1 != endYear) {
            throw new IllegalArgumentException("Invalid assessment year: end year must be start year + 1");
        }
    }

    private JsonNode parseJson(String jsonContent) {
        try {
            return objectMapper.readTree(jsonContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getOriginalMessage());
        }
    }

    private String extractPan(JsonNode json) {
        // Try personalInfo.pan path
        JsonNode personalInfo = json.path("personalInfo");
        if (!personalInfo.isMissingNode() && personalInfo.has("pan")) {
            return personalInfo.get("pan").asText();
        }
        // Try PersonalInfo.PAN path
        JsonNode personalInfoAlt = json.path("PersonalInfo");
        if (!personalInfoAlt.isMissingNode() && personalInfoAlt.has("PAN")) {
            return personalInfoAlt.get("PAN").asText();
        }
        return null;
    }

    private String mapToItr1(JsonNode prefillJson, Client client) {
        try {
            ObjectNode itr1 = objectMapper.createObjectNode();

            // PersonalInfo mapping
            ObjectNode personalInfo = objectMapper.createObjectNode();
            personalInfo.put("AssesseeName", getNestedValue(prefillJson, "personalInfo", "name", client.getName()));
            personalInfo.put("PAN", client.getPan());
            personalInfo.put("DOB", getNestedValue(prefillJson, "personalInfo", "dob",
                    client.getDob() != null ? client.getDob().toString() : ""));
            itr1.set("PersonalInfo", personalInfo);

            // Income & Deductions mapping
            ObjectNode incomeDeductions = objectMapper.createObjectNode();
            double grossSalary = getNestedDouble(prefillJson, "basicDetails", "salary");
            incomeDeductions.put("GrossSalary", grossSalary);

            double otherSources = getNestedDouble(prefillJson, "basicDetails", "otherSources");
            incomeDeductions.put("IncomeFromOtherSources", otherSources);
            itr1.set("ITR1_IncomeDeductions", incomeDeductions);

            // Deductions mapping
            ObjectNode schedule80C = objectMapper.createObjectNode();
            double sec80C = getNestedDouble(prefillJson, "deductions", "section80C");
            schedule80C.put("TotalDeductionAmount", Math.min(sec80C, 150000)); // Max 1.5L
            itr1.set("Schedule80C", schedule80C);

            ObjectNode schedule80D = objectMapper.createObjectNode();
            double sec80D = getNestedDouble(prefillJson, "deductions", "section80D");
            schedule80D.put("TotalDeductionAmount", sec80D);
            itr1.set("Schedule80D", schedule80D);

            // Assessment Year
            ObjectNode partBTTI = objectMapper.createObjectNode();
            partBTTI.put("AssessmentYear", getNestedValue(prefillJson, "basicDetails", "assessmentYear", ""));
            double totalIncome = grossSalary + otherSources - Math.min(sec80C, 150000) - sec80D;
            partBTTI.put("TotalIncome", Math.max(totalIncome, 0));
            itr1.set("ITR1_PartB_TTI", partBTTI);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(itr1);
        } catch (JsonProcessingException e) {
            log.error("Error mapping to ITR-1: {}", e.getMessage());
            return "{}";
        }
    }

    private String getNestedValue(JsonNode json, String parent, String field, String defaultVal) {
        JsonNode parentNode = json.path(parent);
        if (!parentNode.isMissingNode() && parentNode.has(field)) {
            return parentNode.get(field).asText();
        }
        return defaultVal;
    }

    private double getNestedDouble(JsonNode json, String parent, String field) {
        JsonNode parentNode = json.path(parent);
        if (!parentNode.isMissingNode() && parentNode.has(field)) {
            return parentNode.get(field).asDouble(0.0);
        }
        return 0.0;
    }

    private PrefillResponse buildPrefillResponse(Client client, ClientYearData yearData, JsonNode prefillJson) {
        double grossSalary = getNestedDouble(prefillJson, "basicDetails", "salary");
        double otherSources = getNestedDouble(prefillJson, "basicDetails", "otherSources");
        double sec80C = Math.min(getNestedDouble(prefillJson, "deductions", "section80C"), 150000);
        double sec80D = getNestedDouble(prefillJson, "deductions", "section80D");
        double totalIncome = Math.max(grossSalary + otherSources - sec80C - sec80D, 0);

        return PrefillResponse.builder()
                .clientId(client.getId())
                .clientName(client.getName())
                .pan(client.getPan())
                .assessmentYear(yearData.getAssessmentYear())
                .status(yearData.getStatus())
                .recommendedItrType(yearData.getItrType() != null ? yearData.getItrType() : "ITR1")
                .incomeSummary(PrefillResponse.IncomeSummary.builder()
                        .assesseeName(getNestedValue(prefillJson, "personalInfo", "name", client.getName()))
                        .panNumber(client.getPan())
                        .dob(getNestedValue(prefillJson, "personalInfo", "dob",
                                client.getDob() != null ? client.getDob().toString() : ""))
                        .grossSalary(grossSalary)
                        .section80C(sec80C)
                        .section80D(sec80D)
                        .otherSources(otherSources)
                        .totalIncome(totalIncome)
                        .build())
                .build();
    }

    /**
     * Detects the appropriate ITR form type based on the prefill JSON data.
     *
     * ITR-2 is required when any of:
     * - Capital gains income exists
     * - More than one house property
     * - Foreign income/assets
     * - Total income > ₹50 lakhs
     * - Non-resident (NRI/RNOR)
     * - Director in a company
     * - Holds unlisted equity shares
     * - Agricultural income > ₹5,000
     *
     * Otherwise ITR-1 is sufficient.
     */
    private String detectItrType(JsonNode json) {
        // 1. Check for capital gains in insights
        JsonNode scheduleCG = json.path("insights").path("scheduleCG");
        if (!scheduleCG.isMissingNode() && !scheduleCG.isNull()) {
            // Check if there's any non-zero CG amount
            if (hasNonZeroField(scheduleCG)) {
                return "ITR2";
            }
        }

        // Check for CG in form26as (sale of property / securities TDS)
        JsonNode tdsSaleProperty = json.path("form26as").path("tdsOnSaleOfProperty");
        if (!tdsSaleProperty.isMissingNode() && tdsSaleProperty.isArray() && tdsSaleProperty.size() > 0) {
            return "ITR2";
        }

        // Check AIS for capital gains transactions
        JsonNode ais = json.path("ais");
        if (!ais.isMissingNode()) {
            JsonNode sftInfo = ais.path("sftInfo");
            if (checkForCGinAIS(sftInfo)) {
                return "ITR2";
            }
            JsonNode tdsInfo = ais.path("tdsInfo");
            if (checkForCGinAIS(tdsInfo)) {
                return "ITR2";
            }
        }

        // 2. Check residential status — NRI requires ITR-2
        String resStatus = json.path("personalInfo").path("filingStatus")
                .path("residentialStatus").asText(
                        json.path("filingStatus").path("residentialStatus").asText("RES")
                );
        if ("NRI".equalsIgnoreCase(resStatus) || "NRNR".equalsIgnoreCase(resStatus)
                || "NOR".equalsIgnoreCase(resStatus)) {
            return "ITR2";
        }

        // 3. Check multiple house properties
        JsonNode hpArr = json.path("insights").path("scheduleHP");
        if (!hpArr.isMissingNode() && hpArr.isArray() && hpArr.size() > 1) {
            return "ITR2";
        }

        // 4. Check total income > 50 lakhs
        long totalIncome = 0;
        JsonNode cumulSal = json.path("insights").path("cumulativeSalary");
        totalIncome += longVal(cumulSal, "salary") + longVal(cumulSal, "perquisitesValue")
                + longVal(cumulSal, "profitsInSalary");
        // Add salary from form24q if available
        if (totalIncome == 0) {
            JsonNode f24q = json.path("form24q").path("incomeDeductions");
            totalIncome += longVal(f24q, "salary") + longVal(f24q, "perquisitesValue")
                    + longVal(f24q, "profitsInSalary");
        }
        JsonNode insightsOS = json.path("insights");
        totalIncome += longVal(insightsOS, "intrstFrmSavingBank");
        JsonNode schedOS = insightsOS.path("scheduleOS").path("incOthThanOwnRaceHorse");
        totalIncome += longVal(schedOS, "dividendGross");
        if (totalIncome > 5000000) {
            return "ITR2";
        }

        // 5. Check for foreign income / assets
        JsonNode scheduleFSI = json.path("insights").path("scheduleFSI");
        if (!scheduleFSI.isMissingNode() && !scheduleFSI.isNull() && hasNonZeroField(scheduleFSI)) {
            return "ITR2";
        }
        JsonNode scheduleFA = json.path("insights").path("scheduleFA");
        if (!scheduleFA.isMissingNode() && !scheduleFA.isNull()
                && scheduleFA.isArray() && scheduleFA.size() > 0) {
            return "ITR2";
        }

        // 6. Agricultural income > 5,000
        long agriIncome = longVal(json.path("insights"), "agricultureIncome");
        if (agriIncome > 5000) {
            return "ITR2";
        }

        // 7. Check for director / unlisted equity flags
        JsonNode filingStatus = json.path("filingStatus");
        if ("Y".equalsIgnoreCase(filingStatus.path("directorInCompany").asText("N"))) {
            return "ITR2";
        }
        if ("Y".equalsIgnoreCase(filingStatus.path("unlistedEquityShares").asText("N"))) {
            return "ITR2";
        }

        return "ITR1";
    }

    private boolean hasNonZeroField(JsonNode node) {
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                JsonNode val = entry.getValue();
                if (val.isNumber() && val.asLong() != 0) return true;
                if (val.isObject() && hasNonZeroField(val)) return true;
                if (val.isArray()) {
                    for (JsonNode item : val) {
                        if (hasNonZeroField(item)) return true;
                    }
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (hasNonZeroField(item)) return true;
            }
        }
        return false;
    }

    private boolean checkForCGinAIS(JsonNode section) {
        if (section == null || section.isMissingNode() || !section.isArray()) return false;
        for (JsonNode entry : section) {
            String infoCode = entry.path("infoCode").asText(entry.path("transactionCode").asText(""));
            // Common AIS codes for CG: SFT-018 (mutual fund), SFT-005 (shares), etc.
            // Also look for nature of income containing capital gain keywords
            String nature = entry.path("natureOfIncome").asText(
                    entry.path("description").asText("")).toLowerCase();
            if (nature.contains("capital gain") || nature.contains("sale of")
                    || nature.contains("stt") || nature.contains("securities")) {
                return true;
            }
        }
        return false;
    }

    private long longVal(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) return 0;
        return node.path(field).asLong(0);
    }
}
