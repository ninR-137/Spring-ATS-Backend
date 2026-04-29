package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationDto {
 
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}
