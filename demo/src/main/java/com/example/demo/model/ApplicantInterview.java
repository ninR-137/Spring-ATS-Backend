package com.example.demo.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "applicant_interview")
@Getter
@Setter
public class ApplicantInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "scheduled_end_date")
    private LocalDateTime scheduledEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", length = 50)
    private InterviewType interviewType;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "meeting_password", length = 100)
    private String meetingPassword;

    @Column(name = "meeting_id", length = 255)
    private String meetingId;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 60;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "no_show", nullable = false)
    private boolean noShow = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "calendar_event_id", length = 255)
    private String calendarEventId;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "reminders_scheduled")
    private Boolean remindersScheduled = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reminder_24h_sent")
    private LocalDateTime reminder24hSent;

    @Column(name = "reminder_1h_sent")
    private LocalDateTime reminder1hSent;
}
