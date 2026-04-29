package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleOrientationRequest {

    @NotNull(message = "scheduledDate is required")
    private LocalDateTime scheduledDate;

    private Integer durationMinutes = 120;

    private List<String> documentsRequired;

    private Boolean generateMeetingLink = true;

    private Boolean sendEmailNotifications = true;
}
