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
            yearData.setStatus("draft");
        } else {
            // Create new record
            yearData = ClientYearData.builder()
                    .client(client)
                    .assessmentYear(year)
                    .rawPrefillJson(jsonContent)
                    .computedItr1Json(mapToItr1(prefillJson, client))
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
}
