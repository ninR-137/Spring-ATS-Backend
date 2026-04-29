package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRecipientStatusPatchDto {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
