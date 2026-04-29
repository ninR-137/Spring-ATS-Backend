package com.example.demo.dto;

import com.example.demo.model.ApplicantStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicantStatusUpdateDto {

    @NotNull(message = "Status is required")
    private ApplicantStatus status;

    private String notes;
}
