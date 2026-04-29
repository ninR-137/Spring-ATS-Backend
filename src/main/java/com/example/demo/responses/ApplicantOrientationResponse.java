package com.example.demo.responses;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.model.ApplicantOrientation;

import lombok.Getter;

@Getter
public class ApplicantOrientationResponse {

    private final Long id;
    private final Long applicantId;
    private final LocalDateTime scheduledDate;
    private final LocalDateTime scheduledEndDate;
    private final Integer durationMinutes;
    private final String meetingLink;
    private final String meetingPassword;
    private final String calendarEventId;
    private final List<String> documentsRequired;
    private final boolean completed;
    private final boolean noShow;
    private final String notes;

    public ApplicantOrientationResponse(ApplicantOrientation orientation) {
        this.id = orientation.getId();
        this.applicantId = orientation.getApplicant() == null ? null : orientation.getApplicant().getId();
        this.scheduledDate = orientation.getScheduledDate();
        this.scheduledEndDate = orientation.getScheduledEndDate();
        this.durationMinutes = orientation.getDurationMinutes();
        this.meetingLink = orientation.getMeetingLink();
        this.meetingPassword = orientation.getMeetingPassword();
        this.calendarEventId = orientation.getCalendarEventId();
        this.documentsRequired = orientation.getDocumentsRequired();
        this.completed = orientation.isCompleted();
        this.noShow = orientation.isNoShow();
        this.notes = orientation.getNotes();
    }
}
