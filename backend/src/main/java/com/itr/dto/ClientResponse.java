package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClientResponse {
    private Long id;
    private String pan;
    private String name;
    private String email;
    private String mobile;
    private String aadhaar;
    private LocalDate dob;
    private List<YearStatus> years;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class YearStatus {
        private String year;
        private String status;
    }
}
