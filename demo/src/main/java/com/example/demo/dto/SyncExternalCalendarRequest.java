package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyncExternalCalendarRequest {

    @NotNull(message = "facilityId is required")
    private Long facilityId;

    @NotBlank(message = "calendarType is required")
    private String calendarType;

    @NotBlank(message = "accessToken is required")
    private String accessToken;

    private String refreshToken;
}
