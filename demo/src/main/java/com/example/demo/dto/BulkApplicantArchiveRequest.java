package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkApplicantArchiveRequest {

    @NotEmpty(message = "Applicant IDs are required")
    private List<Long> applicantIds;
}
