package com.example.demo.responses;

import java.time.LocalDateTime;

import com.example.demo.model.ApplicantInterview;

import lombok.Getter;

@Getter
public class ApplicantInterviewResponse {

    private final Long id;
    private final Long applicantId;
    private final LocalDateTime scheduledDate;
    private final LocalDateTime scheduledEndDate;
    private final Integer durationMinutes;
    private final String interviewType;
    private final String meetingLink;
    private final String meetingPassword;
    private final String meetingId;
    private final String calendarEventId;
    private final String notes;
    private final boolean completed;
    private final boolean noShow;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private final Boolean emailSent;
    private final Boolean remindersScheduled;

    public ApplicantInterviewResponse(ApplicantInterview interview) {
        this.id = interview.getId();
        this.applicantId = interview.getApplicant() == null ? null : interview.getApplicant().getId();
        this.scheduledDate = interview.getScheduledDate();
        this.scheduledEndDate = interview.getScheduledEndDate();
        this.durationMinutes = interview.getDurationMinutes();
        this.interviewType = interview.getInterviewType() == null ? null : interview.getInterviewType().name();
        this.meetingLink = interview.getMeetingLink();
        this.meetingPassword = interview.getMeetingPassword();
        this.meetingId = interview.getMeetingId();
        this.calendarEventId = interview.getCalendarEventId();
        this.notes = interview.getNotes();
        this.completed = interview.isCompleted();
        this.noShow = interview.isNoShow();
        this.completedAt = interview.getCompletedAt();
        this.createdAt = interview.getCreatedAt();
        this.emailSent = interview.getEmailSent();
        this.remindersScheduled = interview.getRemindersScheduled();
    }
}
