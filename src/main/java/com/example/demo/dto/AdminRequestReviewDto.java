package com.example.demo.dto;

import com.example.demo.model.AdminRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRequestReviewDto {

    @NotNull(message = "Status is required")
    private AdminRequestStatus status;
}