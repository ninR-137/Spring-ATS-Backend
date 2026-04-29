package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "applicant_orientation")
@Getter
@Setter
public class ApplicantOrientation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false, unique = true)
    private Applicant applicant;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "scheduled_end_date")
    private LocalDateTime scheduledEndDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 120;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "meeting_password", length = 100)
    private String meetingPassword;

    @Column(name = "calendar_event_id", length = 255)
    private String calendarEventId;

    @ElementCollection
    @CollectionTable(name = "applicant_orientation_documents", joinColumns = @JoinColumn(name = "orientation_id"))
    @Column(name = "document_name")
    private List<String> documentsRequired = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "no_show", nullable = false)
    private boolean noShow = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
