package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApplicantStatusUpdateDto;
import com.example.demo.dto.BulkApplicantArchiveRequest;
import com.example.demo.dto.BulkStatusUpdateRequest;
import com.example.demo.dto.CompleteInterviewRequest;
import com.example.demo.dto.CompleteOrientationRequest;
import com.example.demo.dto.CreateApplicantRequest;
import com.example.demo.dto.ResendNotificationRequest;
import com.example.demo.dto.ScheduleInterviewRequest;
import com.example.demo.dto.ScheduleOrientationRequest;
import com.example.demo.dto.UpdateApplicantRequest;
import com.example.demo.model.ApplicantStatus;
import com.example.demo.model.User;
import com.example.demo.responses.ApplicantInterviewResponse;
import com.example.demo.responses.ApplicantOrientationResponse;
import com.example.demo.responses.ApplicantResponse;
import com.example.demo.responses.ApplicantStatsResponse;
import com.example.demo.responses.BulkDeletedApplicantResponse;
import com.example.demo.responses.BulkRecoveredApplicantResponse;
import com.example.demo.responses.BulkStatusUpdateResponse;
import com.example.demo.responses.DeletedApplicantResponse;
import com.example.demo.responses.NotificationResponse;
import com.example.demo.responses.RecoveredApplicantResponse;
import com.example.demo.service.ApplicantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/applicants")
public class ApplicantController {

    private final ApplicantService applicantService;

    public ApplicantController(ApplicantService applicantService) {
        this.applicantService = applicantService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicantResponse> createApplicant(
        @AuthenticationPrincipal User currentUser,
        @Valid @ModelAttribute CreateApplicantRequest request,
        @RequestParam("resume") MultipartFile resume
    ) {
        ApplicantResponse response = applicantService.createApplicant(currentUser, request, resume);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ApplicantResponse>> getApplicants(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Long facilityId,
        @RequestParam(required = false) ApplicantStatus status,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "addedDate,desc") String sort
    ) {
        Page<ApplicantResponse> response = applicantService.getApplicants(
            currentUser,
            facilityId,
            status,
            role,
            fromDate,
            toDate,
            page,
            size,
            sort
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{applicantId}")
    public ResponseEntity<ApplicantResponse> getApplicantById(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId
    ) {
        return ResponseEntity.ok(applicantService.getApplicantById(currentUser, applicantId));
    }

    @PatchMapping("/{applicantId}/status")
    public ResponseEntity<ApplicantResponse> updateApplicantStatus(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId,
        @Valid @RequestBody ApplicantStatusUpdateDto request
    ) {
        return ResponseEntity.ok(applicantService.updateApplicantStatus(currentUser, applicantId, request));
    }

    @PutMapping(value = "/{applicantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicantResponse> updateApplicant(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId,
        @Valid @ModelAttribute UpdateApplicantRequest request,
        @RequestParam(value = "resume", required = false) MultipartFile resume
    ) {
        return ResponseEntity.ok(applicantService.updateApplicant(currentUser, applicantId, request, resume));
    }

    @GetMapping("/{applicantId}/resume")
    public ResponseEntity<Resource> downloadResume(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId
    ) {
        ApplicantService.ResumeDownload resume = applicantService.getResume(currentUser, applicantId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(resume.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resume.filename() + "\"")
            .body(resume.resource());
    }

    @PostMapping("/{applicantId}/interviews")
    public ResponseEntity<ApplicantInterviewResponse> scheduleInterview(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId,
        @Valid @RequestBody ScheduleInterviewRequest request
    ) {
        ApplicantInterviewResponse response = applicantService.scheduleInterview(currentUser, applicantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{applicantId}/interviews/{interviewId}/complete")
    public ResponseEntity<ApplicantInterviewResponse> completeInterview(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId,
        @PathVariable Long interviewId,
        @Valid @RequestBody CompleteInterviewRequest request
    ) {
        ApplicantInterviewResponse response = applicantService.completeInterview(
            currentUser,
            applicantId,
            interviewId,
            request
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{applicantId}/orientation")
    public ResponseEntity<ApplicantOrientationResponse> scheduleOrientation(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId,
        @Valid @RequestBody ScheduleOrientationRequest request
    ) {
        ApplicantOrientationResponse response = applicantService.scheduleOrientation(currentUser, applicantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{applicantId}/orientation/{orientationId}/complete")
    public ResponseEntity<ApplicantOrientationResponse> completeOrientation(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId,
        @PathVariable Long orientationId,
        @Valid @RequestBody CompleteOrientationRequest request
    ) {
        return ResponseEntity.ok(applicantService.completeOrientation(currentUser, applicantId, orientationId, request));
    }

    @PatchMapping("/bulk/status")
    public ResponseEntity<BulkStatusUpdateResponse> bulkUpdateStatus(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody BulkStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(applicantService.bulkUpdateStatus(currentUser, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApplicantStatsResponse> getStats(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Long facilityId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(applicantService.getStats(currentUser, facilityId, fromDate, toDate));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ApplicantResponse>> searchApplicants(
        @AuthenticationPrincipal User currentUser,
        @RequestParam("q") String query,
        @RequestParam(required = false) Long facilityId,
        @RequestParam(required = false) ApplicantStatus status,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(applicantService.searchApplicants(currentUser, query, facilityId, status, limit));
    }

    @PostMapping("/{applicantId}/resend-notification")
    public ResponseEntity<NotificationResponse> resendNotification(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId,
        @Valid @RequestBody ResendNotificationRequest request
    ) {
        return ResponseEntity.ok(applicantService.resendNotification(currentUser, applicantId, request));
    }

    @DeleteMapping("/{applicantId}")
    public ResponseEntity<DeletedApplicantResponse> archiveApplicant(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId
    ) {
        return ResponseEntity.ok(applicantService.archiveApplicant(currentUser, applicantId));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<BulkDeletedApplicantResponse> archiveApplicantsBulk(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody BulkApplicantArchiveRequest request
    ) {
        return ResponseEntity.ok(applicantService.archiveApplicantsBulk(currentUser, request));
    }

    @PostMapping("/{applicantId}/recover")
    public ResponseEntity<RecoveredApplicantResponse> recoverApplicant(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long applicantId
    ) {
        return ResponseEntity.ok(applicantService.recoverApplicant(currentUser, applicantId));
    }

    @PostMapping("/bulk/recover")
    public ResponseEntity<BulkRecoveredApplicantResponse> recoverApplicantsBulk(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody BulkApplicantArchiveRequest request
    ) {
        return ResponseEntity.ok(applicantService.recoverApplicantsBulk(currentUser, request));
    }
}
