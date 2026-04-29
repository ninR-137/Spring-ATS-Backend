package com.example.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleInterviewRequest {

    @NotNull(message = "Scheduled date is required")
    private LocalDateTime scheduledDate;

    private Integer durationMinutes = 60;

    @NotBlank(message = "Interview type is required")
    private String interviewType;

    private String notes;

    private Boolean generateMeetingLink = true;

    private Boolean sendCalendarInvites = true;

    private Boolean sendEmailNotifications = true;
}
