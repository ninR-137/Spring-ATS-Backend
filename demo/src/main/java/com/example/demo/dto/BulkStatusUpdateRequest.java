package com.example.demo.dto;

import java.util.List;

import com.example.demo.model.ApplicantStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkStatusUpdateRequest {

    @NotEmpty(message = "Applicant IDs are required")
    private List<Long> applicantIds;

    @NotNull(message = "Status is required")
    private ApplicantStatus status;

    private String notes;
}
