package com.example.demo.dto;

import com.example.demo.model.ApplicantStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteInterviewRequest {

    @NotNull(message = "completed is required")
    private Boolean completed;

    private Boolean noShow = false;

    private String notes;

    private ApplicantStatus newStatus;
}
