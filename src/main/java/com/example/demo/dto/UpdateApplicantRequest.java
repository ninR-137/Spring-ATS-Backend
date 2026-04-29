package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateApplicantRequest {

    private String name;

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    private String role;

    private String notes;
}
