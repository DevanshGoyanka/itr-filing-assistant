package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrefillResponse {
    private Long clientId;
    private String clientName;
    private String pan;
    private String assessmentYear;
    private String status;
    private IncomeSummary incomeSummary;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IncomeSummary {
        private String assesseeName;
        private String panNumber;
        private String dob;
        private Double grossSalary;
        private Double section80C;
        private Double section80D;
        private Double totalIncome;
        private Double otherSources;
    }
}
