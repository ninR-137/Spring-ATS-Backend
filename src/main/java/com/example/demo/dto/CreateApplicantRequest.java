package com.example.demo.dto;

import com.example.demo.model.ApplicantStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateApplicantRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    @NotBlank(message = "Role is required")
    private String role;

    @NotNull(message = "Facility ID is required")
    private Long facilityId;

    private ApplicantStatus status;

    private String notes;
}
