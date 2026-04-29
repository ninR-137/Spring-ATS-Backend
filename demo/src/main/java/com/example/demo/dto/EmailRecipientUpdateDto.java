package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRecipientUpdateDto {

    @Email(message = "Email should be valid")
    private String email;

    private String name;

    private Boolean isActive;
}