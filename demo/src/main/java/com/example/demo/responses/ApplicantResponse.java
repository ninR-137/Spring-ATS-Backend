package com.example.demo.responses;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.model.Applicant;

import lombok.Getter;

@Getter
public class ApplicantResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final String role;
    private final Long facilityId;
    private final String facilityName;
    private final String status;
    private final String resumeUrl;
    private final LocalDateTime addedDate;
    private final LocalDateTime lastUpdated;
    private final Long addedBy;
    private final String addedByName;
    private final String notes;
    private final boolean archived;
    private final LocalDateTime archivedAt;
    private final List<ApplicantInterviewResponse> interviews;
    private final ApplicantOrientationResponse orientation;

    public ApplicantResponse(
        Applicant applicant,
        List<ApplicantInterviewResponse> interviews,
        ApplicantOrientationResponse orientation
    ) {
        this.id = applicant.getId();
        this.name = applicant.getName();
        this.email = applicant.getEmail();
        this.phoneNumber = applicant.getPhoneNumber();
        this.role = applicant.getRole();
        this.facilityId = applicant.getFacility() == null ? null : applicant.getFacility().getId();
        this.facilityName = applicant.getFacility() == null ? null : applicant.getFacility().getName();
        this.status = applicant.getStatus().name();
        this.resumeUrl = applicant.getResumeUrl();
        this.addedDate = applicant.getAddedDate();
        this.lastUpdated = applicant.getUpdatedDate();
        this.addedBy = applicant.getAddedBy() == null ? null : applicant.getAddedBy().getId();
        this.addedByName = applicant.getAddedBy() == null ? null : applicant.getAddedBy().getUsername();
        this.notes = applicant.getNotes();
        this.archived = applicant.isArchived();
        this.archivedAt = applicant.getArchivedAt();
        this.interviews = interviews;
        this.orientation = orientation;
    }
}
