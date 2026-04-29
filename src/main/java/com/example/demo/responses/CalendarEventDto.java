package com.example.demo.responses;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarEventDto {

    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private Boolean allDay = false;
    private String status;
    private String interviewType;
    private CalendarApplicantDto applicant;
    private String meetingLink;
    private String meetingPassword;
    private String meetingId;
    private String calendarEventId;
    private String notes;
    private Map<String, Boolean> remindersSent;
    private CompletionInfoDto completionInfo;

    // For upcoming view
    private String timeUntil;

    // For week/day view compact fields
    private String startTime;
    private String endTime;
    private String type;
    private String time;
    private Integer duration;

    @Getter
    @Setter
    public static class CalendarApplicantDto {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private String role;
        private String resumeUrl;
    }

    @Getter
    @Setter
    public static class CompletionInfoDto {
        private boolean completed;
        private boolean noShow;
        private LocalDateTime completedAt;
        private String completionNotes;
    }
}
