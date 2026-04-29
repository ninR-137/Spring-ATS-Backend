package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteOrientationRequest {

    @NotNull(message = "completed is required")
    private Boolean completed;

    private Boolean noShow = false;

    private String notes;
}
