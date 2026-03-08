package com.itr.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ClientRequest {
    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format. Must be like ABCDE1234F")
    private String pan;

    @NotBlank(message = "Name is required")
    @Size(max = 125, message = "Name must not exceed 125 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 15, message = "Mobile must not exceed 15 characters")
    private String mobile;

    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Aadhaar must be exactly 12 numeric digits")
    private String aadhaar;

    private LocalDate dob;
}
