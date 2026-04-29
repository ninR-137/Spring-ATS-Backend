package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Applicant;
import com.example.demo.model.ApplicantInterview;
import com.example.demo.model.ApplicantOrientation;
import com.example.demo.model.ApplicantStatus;
import com.example.demo.model.Facility;
import com.example.demo.model.InterviewType;
import com.example.demo.model.NotificationType;
import com.example.demo.model.User;
import com.example.demo.repository.ApplicantInterviewRepository;
import com.example.demo.repository.ApplicantOrientationRepository;
import com.example.demo.repository.ApplicantRepository;
import com.example.demo.repository.EmailRecipientRepository;
import com.example.demo.repository.FacilityRepository;
import com.example.demo.responses.ApplicantInterviewResponse;
import com.example.demo.responses.ApplicantOrientationResponse;
import com.example.demo.responses.ApplicantResponse;
import com.example.demo.responses.ApplicantStatsResponse;
import com.example.demo.responses.BulkDeletedApplicantResponse;
import com.example.demo.responses.BulkRecoveredApplicantResponse;
import com.example.demo.responses.BulkStatusUpdateResponse;
import com.example.demo.responses.DeletedApplicantResponse;
import com.example.demo.responses.FacilityCountResponse;
import com.example.demo.responses.NotificationResponse;
import com.example.demo.responses.RecoveredApplicantResponse;

@Service
public class ApplicantService {

    private static final int MAX_SEARCH_LIMIT = 100;

    private final ApplicantRepository applicantRepository;
    private final ApplicantInterviewRepository interviewRepository;
    private final ApplicantOrientationRepository orientationRepository;
    private final FacilityRepository facilityRepository;
    private final EmailRecipientRepository emailRecipientRepository;
    private final ApplicantPermissionService permissionService;
    private final ResumeStorageService resumeStorageService;
    private final MeetingLinkService meetingLinkService;
    private final CalendarService calendarService;
    private final EmailNotificationService emailNotificationService;

    public ApplicantService(
        ApplicantRepository applicantRepository,
        ApplicantInterviewRepository interviewRepository,
        ApplicantOrientationRepository orientationRepository,
        FacilityRepository facilityRepository,
        EmailRecipientRepository emailRecipientRepository,
        ApplicantPermissionService permissionService,
        ResumeStorageService resumeStorageService,
        MeetingLinkService meetingLinkService,
        CalendarService calendarService,
        EmailNotificationService emailNotificationService
    ) {
        this.applicantRepository = applicantRepository;
        this.interviewRepository = interviewRepository;
        this.orientationRepository = orientationRepository;
        this.facilityRepository = facilityRepository;
        this.emailRecipientRepository = emailRecipientRepository;
        this.permissionService = permissionService;
        this.resumeStorageService = resumeStorageService;
        this.meetingLinkService = meetingLinkService;
        this.calendarService = calendarService;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public ApplicantResponse createApplicant(User currentUser, CreateApplicantRequest request, MultipartFile resume) {
        permissionService.validateFacilityAccess(currentUser, request.getFacilityId());

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (applicantRepository.existsByFacilityIdAndEmailIgnoreCaseAndIsArchivedFalse(
            request.getFacilityId(),
            normalizedEmail
        )) {
            throw new IllegalArgumentException("Duplicate applicant email for this facility");
        }

        Facility facility = facilityRepository.findById(request.getFacilityId())
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));

        Applicant applicant = new Applicant();
        applicant.setName(requireTrimmed(request.getName(), "Name is required"));
        applicant.setEmail(normalizedEmail);
        applicant.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        applicant.setRole(requireTrimmed(request.getRole(), "Role is required"));
        applicant.setFacility(facility);
        applicant.setStatus(request.getStatus() == null ? ApplicantStatus.INVITING_FOR_INTERVIEW : request.getStatus());
        applicant.setNotes(trimToNull(request.getNotes()));
        applicant.setAddedBy(currentUser);

        Applicant saved = applicantRepository.save(applicant);
        String resumePath = resumeStorageService.storeResume(resume, facility.getId(), saved.getId());
        saved.setResumeUrl(resumePath);
        saved.setResumeContentType(resume.getContentType());

        saved = applicantRepository.save(saved);
        return toResponse(saved, false, false);
    }

    @Transactional(readOnly = true)
    public Page<ApplicantResponse> getApplicants(
        User currentUser,
        Long facilityId,
        ApplicantStatus status,
        String role,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size,
        String sort
    ) {
        ensureFacilityScoping(currentUser, facilityId);
        validateDateRange(fromDate, toDate);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), parseSort(sort));
        Specification<Applicant> specification = buildFilterSpec(facilityId, status, role, fromDate, toDate);

        return applicantRepository.findAll(specification, pageable)
            .map(applicant -> toResponse(applicant, false, false));
    }

    @Transactional(readOnly = true)
    public ApplicantResponse getApplicantById(User currentUser, Long applicantId) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);
        return toResponse(applicant, true, true);
    }

    @Transactional
    public ApplicantResponse updateApplicantStatus(User currentUser, Long applicantId, ApplicantStatusUpdateDto request) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        validateStatusTransition(applicant.getStatus(), request.getStatus());
        applicant.setStatus(request.getStatus());

        if (request.getStatus() == ApplicantStatus.HIRED && applicant.getHiredAt() == null) {
            applicant.setHiredAt(LocalDateTime.now());
        }

        if (request.getNotes() != null) {
            applicant.setNotes(trimToNull(request.getNotes()));
        }

        Applicant saved = applicantRepository.save(applicant);

        emailNotificationService.sendStatusUpdate(saved, saved.getStatus().name(), request.getNotes());
        if (saved.getStatus() == ApplicantStatus.HIRED) {
            emailNotificationService.sendHiredEmail(saved);
        }

        return toResponse(saved, false, false);
    }

    @Transactional
    public ApplicantResponse updateApplicant(User currentUser, Long applicantId, UpdateApplicantRequest request, MultipartFile resume) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        if (request.getName() != null) {
            applicant.setName(requireTrimmed(request.getName(), "Name cannot be blank"));
        }

        if (request.getEmail() != null) {
            String normalizedEmail = normalizeEmail(request.getEmail());
            if (applicantRepository.existsByFacilityIdAndEmailIgnoreCaseAndIdNotAndIsArchivedFalse(
                applicant.getFacility().getId(), normalizedEmail, applicant.getId()
            )) {
                throw new IllegalArgumentException("Duplicate applicant email for this facility");
            }
            applicant.setEmail(normalizedEmail);
        }

        if (request.getPhoneNumber() != null) {
            applicant.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        }

        if (request.getRole() != null) {
            applicant.setRole(requireTrimmed(request.getRole(), "Role cannot be blank"));
        }

        if (request.getNotes() != null) {
            applicant.setNotes(trimToNull(request.getNotes()));
        }

        if (resume != null && !resume.isEmpty()) {
            String newResumePath = resumeStorageService.storeResume(
                resume,
                applicant.getFacility().getId(),
                applicant.getId()
            );
            applicant.setResumeUrl(newResumePath);
            applicant.setResumeContentType(resume.getContentType());
        }

        return toResponse(applicantRepository.save(applicant), false, false);
    }

    @Transactional(readOnly = true)
    public ResumeDownload getResume(User currentUser, Long applicantId) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        if (!StringUtils.hasText(applicant.getResumeUrl())) {
            throw new ResourceNotFoundException("Resume not found");
        }

        Resource resource = resumeStorageService.loadResumeAsResource(applicant.getResumeUrl());
        String fileName = applicant.getResumeUrl().substring(applicant.getResumeUrl().lastIndexOf('/') + 1);
        String contentType = StringUtils.hasText(applicant.getResumeContentType())
            ? applicant.getResumeContentType()
            : "application/octet-stream";
        return new ResumeDownload(resource, contentType, fileName);
    }

    @Transactional
    public ApplicantInterviewResponse scheduleInterview(User currentUser, Long applicantId, ScheduleInterviewRequest request) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        InterviewType interviewType = parseInterviewType(request.getInterviewType());
        int durationMinutes = normalizeDuration(request.getDurationMinutes(), 60);

        ApplicantInterview interview = new ApplicantInterview();
        interview.setApplicant(applicant);
        interview.setScheduledDate(request.getScheduledDate());
        interview.setDurationMinutes(durationMinutes);
        interview.setScheduledEndDate(request.getScheduledDate().plusMinutes(durationMinutes));
        interview.setInterviewType(interviewType);
        interview.setNotes(trimToNull(request.getNotes()));

        if (Boolean.TRUE.equals(request.getGenerateMeetingLink())) {
            interview.setMeetingLink(meetingLinkService.generateMeetingLink(interview));
            interview.setMeetingPassword(meetingLinkService.randomPassword());
            interview.setMeetingId("m-" + System.nanoTime());
        }

        if (Boolean.TRUE.equals(request.getSendCalendarInvites())) {
            interview.setCalendarEventId(calendarService.addInterviewToCalendar(
                applicant,
                interview,
                emailRecipientRepository.findByFacilityIdAndIsActive(applicant.getFacility().getId(), true)
            ));
        }

        ApplicantInterview savedInterview = interviewRepository.save(interview);

        validateStatusTransition(applicant.getStatus(), ApplicantStatus.SCHEDULED);
        applicant.setStatus(ApplicantStatus.SCHEDULED);
        applicantRepository.save(applicant);

        boolean emailSent = false;
        if (Boolean.TRUE.equals(request.getSendEmailNotifications())) {
            emailSent = emailNotificationService.sendScheduledInterviewEmail(applicant, savedInterview);
        }

        savedInterview.setEmailSent(emailSent);
        savedInterview.setRemindersScheduled(Boolean.TRUE.equals(request.getSendEmailNotifications()));

        return new ApplicantInterviewResponse(interviewRepository.save(savedInterview));
    }

    @Transactional
    public ApplicantInterviewResponse completeInterview(
        User currentUser,
        Long applicantId,
        Long interviewId,
        CompleteInterviewRequest request
    ) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        ApplicantInterview interview = interviewRepository.findByIdAndApplicantId(interviewId, applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        interview.setCompleted(Boolean.TRUE.equals(request.getCompleted()));
        interview.setNoShow(Boolean.TRUE.equals(request.getNoShow()));
        if (interview.isCompleted() && interview.isNoShow()) {
            throw new IllegalArgumentException("Interview cannot be completed and no-show together");
        }

        if (request.getNotes() != null) {
            interview.setNotes(trimToNull(request.getNotes()));
        }

        if (interview.isCompleted() || interview.isNoShow()) {
            interview.setCompletedAt(LocalDateTime.now());
        }

        ApplicantStatus statusToApply = request.getNewStatus();
        if (statusToApply == null) {
            if (interview.isNoShow()) {
                statusToApply = ApplicantStatus.INTERVIEW_NO_SHOW;
            } else if (interview.isCompleted()) {
                statusToApply = ApplicantStatus.INTERVIEW_COMPLETED;
            }
        }

        if (statusToApply != null) {
            validateStatusTransition(applicant.getStatus(), statusToApply);
            applicant.setStatus(statusToApply);
            if (statusToApply == ApplicantStatus.HIRED && applicant.getHiredAt() == null) {
                applicant.setHiredAt(LocalDateTime.now());
            }
            applicantRepository.save(applicant);
        }

        ApplicantInterview saved = interviewRepository.save(interview);
        emailNotificationService.sendInterviewCompletedEmail(applicant, saved);
        if (applicant.getStatus() == ApplicantStatus.HIRED) {
            emailNotificationService.sendHiredEmail(applicant);
        }
        return new ApplicantInterviewResponse(saved);
    }

    @Transactional
    public ApplicantOrientationResponse scheduleOrientation(User currentUser, Long applicantId, ScheduleOrientationRequest request) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        int durationMinutes = normalizeDuration(request.getDurationMinutes(), 120);

        ApplicantOrientation orientation = orientationRepository.findByApplicantId(applicantId)
            .orElseGet(ApplicantOrientation::new);
        orientation.setApplicant(applicant);
        orientation.setScheduledDate(request.getScheduledDate());
        orientation.setDurationMinutes(durationMinutes);
        orientation.setScheduledEndDate(request.getScheduledDate().plusMinutes(durationMinutes));
        orientation.setDocumentsRequired(request.getDocumentsRequired() == null ? List.of() : request.getDocumentsRequired());

        if (Boolean.TRUE.equals(request.getGenerateMeetingLink())) {
            orientation.setMeetingLink(meetingLinkService.generateOrientationLink(orientation));
            orientation.setMeetingPassword(meetingLinkService.randomPassword());
        }

        orientation.setCalendarEventId(calendarService.addOrientationToCalendar(
            applicant,
            orientation,
            emailRecipientRepository.findByFacilityIdAndIsActive(applicant.getFacility().getId(), true)
        ));

        ApplicantOrientation savedOrientation = orientationRepository.save(orientation);

        validateStatusTransition(applicant.getStatus(), ApplicantStatus.ORIENTATION_SCHEDULED);
        applicant.setStatus(ApplicantStatus.ORIENTATION_SCHEDULED);
        applicantRepository.save(applicant);

        if (Boolean.TRUE.equals(request.getSendEmailNotifications())) {
            emailNotificationService.sendOrientationScheduledEmail(applicant, savedOrientation);
        }

        return new ApplicantOrientationResponse(savedOrientation);
    }

    @Transactional
    public ApplicantOrientationResponse completeOrientation(
        User currentUser,
        Long applicantId,
        Long orientationId,
        CompleteOrientationRequest request
    ) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        ApplicantOrientation orientation = orientationRepository.findByIdAndApplicantId(orientationId, applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Orientation not found"));

        orientation.setCompleted(Boolean.TRUE.equals(request.getCompleted()));
        orientation.setNoShow(Boolean.TRUE.equals(request.getNoShow()));
        if (orientation.isCompleted() && orientation.isNoShow()) {
            throw new IllegalArgumentException("Orientation cannot be completed and no-show together");
        }

        orientation.setNotes(trimToNull(request.getNotes()));
        if (orientation.isCompleted() || orientation.isNoShow()) {
            orientation.setCompletedAt(LocalDateTime.now());
        }

        ApplicantStatus nextStatus = null;
        if (orientation.isCompleted()) {
            nextStatus = ApplicantStatus.HIRED;
        } else if (orientation.isNoShow()) {
            nextStatus = ApplicantStatus.ORIENTATION_NO_SHOW;
        }

        if (nextStatus != null) {
            validateStatusTransition(applicant.getStatus(), nextStatus);
            applicant.setStatus(nextStatus);
            if (nextStatus == ApplicantStatus.HIRED && applicant.getHiredAt() == null) {
                applicant.setHiredAt(LocalDateTime.now());
            }
            applicantRepository.save(applicant);
        }

        ApplicantOrientation saved = orientationRepository.save(orientation);

        if (applicant.getStatus() == ApplicantStatus.HIRED) {
            emailNotificationService.sendHiredEmail(applicant);
        } else {
            emailNotificationService.sendStatusUpdate(applicant, applicant.getStatus().name(), request.getNotes());
        }

        return new ApplicantOrientationResponse(saved);
    }

    @Transactional
    public BulkStatusUpdateResponse bulkUpdateStatus(User currentUser, BulkStatusUpdateRequest request) {
        permissionService.ensureAdmin(currentUser);

        List<Applicant> applicants = applicantRepository.findAllById(request.getApplicantIds());
        Map<Long, Applicant> byId = new HashMap<>();
        for (Applicant applicant : applicants) {
            byId.put(applicant.getId(), applicant);
        }

        List<Long> failedIds = new ArrayList<>();
        int updated = 0;

        for (Long id : request.getApplicantIds()) {
            Applicant applicant = byId.get(id);
            if (applicant == null) {
                failedIds.add(id);
                continue;
            }

            try {
                validateStatusTransition(applicant.getStatus(), request.getStatus());
                applicant.setStatus(request.getStatus());
                if (request.getStatus() == ApplicantStatus.HIRED && applicant.getHiredAt() == null) {
                    applicant.setHiredAt(LocalDateTime.now());
                }
                if (request.getNotes() != null) {
                    applicant.setNotes(trimToNull(request.getNotes()));
                }
                applicantRepository.save(applicant);
                if (request.getStatus() == ApplicantStatus.HIRED) {
                    emailNotificationService.sendHiredEmail(applicant);
                } else {
                    emailNotificationService.sendStatusUpdate(applicant, request.getStatus().name(), request.getNotes());
                }
                updated++;
            } catch (Exception ex) {
                failedIds.add(id);
            }
        }

        String message = failedIds.isEmpty()
            ? "Statuses updated successfully"
            : "Partial update completed with some failures";

        return new BulkStatusUpdateResponse(updated, failedIds, message);
    }

    @Transactional(readOnly = true)
    public ApplicantStatsResponse getStats(User currentUser, Long facilityId, LocalDate fromDate, LocalDate toDate) {
        ensureFacilityScoping(currentUser, facilityId);
        validateDateRange(fromDate, toDate);

        Specification<Applicant> spec = buildFilterSpec(facilityId, null, null, fromDate, toDate);
        List<Applicant> applicants = applicantRepository.findAll(spec);

        Map<String, Long> byStatus = new HashMap<>();
        Map<String, Long> byRole = new HashMap<>();
        Map<String, FacilityCountResponse> byFacilityMap = new HashMap<>();

        long totalDaysToHire = 0;
        long hiredCount = 0;

        for (Applicant applicant : applicants) {
            byStatus.merge(applicant.getStatus().name(), 1L, Long::sum);
            byRole.merge(applicant.getRole(), 1L, Long::sum);

            String facilityKey = String.valueOf(applicant.getFacility().getId());
            FacilityCountResponse current = byFacilityMap.get(facilityKey);
            if (current == null) {
                byFacilityMap.put(
                    facilityKey,
                    new FacilityCountResponse(applicant.getFacility().getId(), applicant.getFacility().getName(), 1L)
                );
            } else {
                byFacilityMap.put(
                    facilityKey,
                    new FacilityCountResponse(current.getFacilityId(), current.getFacilityName(), current.getCount() + 1)
                );
            }

            if (applicant.getStatus() == ApplicantStatus.HIRED && applicant.getHiredAt() != null && applicant.getAddedDate() != null) {
                totalDaysToHire += Duration.between(applicant.getAddedDate(), applicant.getHiredAt()).toDays();
                hiredCount++;
            }
        }

        long total = applicants.size();
        long hired = byStatus.getOrDefault(ApplicantStatus.HIRED.name(), 0L);
        double conversionRate = total == 0 ? 0 : percentage(hired, total);
        double averageTimeToHire = hiredCount == 0 ? 0 : round2(totalDaysToHire / (double) hiredCount);

        ApplicantStatsResponse response = new ApplicantStatsResponse();
        response.setTotalApplicants(total);
        response.setByStatus(byStatus);
        response.setByRole(byRole);
        response.setByFacility(new ArrayList<>(byFacilityMap.values()));
        response.setConversionRate(conversionRate);
        response.setAverageTimeToHire(averageTimeToHire);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ApplicantResponse> searchApplicants(
        User currentUser,
        String query,
        Long facilityId,
        ApplicantStatus status,
        int limit
    ) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("q is required");
        }

        ensureFacilityScoping(currentUser, facilityId);

        int normalizedLimit = Math.min(Math.max(limit, 1), MAX_SEARCH_LIMIT);
        Pageable pageable = PageRequest.of(0, normalizedLimit);

        return applicantRepository.searchApplicants(query.trim(), facilityId, status, pageable)
            .stream()
            .map(applicant -> toResponse(applicant, false, false))
            .toList();
    }

    @Transactional
    public NotificationResponse resendNotification(User currentUser, Long applicantId, ResendNotificationRequest request) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        NotificationType type = request.getNotificationType();
        boolean emailSent = false;
        boolean calendarInviteSent = false;

        switch (type) {
            case INTERVIEW_SCHEDULED -> {
                ApplicantInterview interview = interviewRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No interview available for notification"));
                emailSent = emailNotificationService.sendScheduledInterviewEmail(applicant, interview);
                calendarInviteSent = StringUtils.hasText(interview.getCalendarEventId());
            }
            case ORIENTATION_SCHEDULED -> {
                ApplicantOrientation orientation = orientationRepository.findByApplicantId(applicantId)
                    .orElseThrow(() -> new IllegalArgumentException("No orientation available for notification"));
                emailSent = emailNotificationService.sendOrientationScheduledEmail(applicant, orientation);
                calendarInviteSent = StringUtils.hasText(orientation.getCalendarEventId());
            }
            case HIRED -> {
                if (applicant.getStatus() != ApplicantStatus.HIRED) {
                    throw new IllegalArgumentException("Applicant is not in HIRED status");
                }
                emailSent = emailNotificationService.sendHiredEmail(applicant);
            }
            case STATUS_UPDATE -> emailSent = emailNotificationService.sendStatusUpdate(applicant, applicant.getStatus().name(), applicant.getNotes());
            default -> throw new IllegalArgumentException("Unsupported notification type");
        }

        return new NotificationResponse("Notification processed", emailSent, calendarInviteSent);
    }

    @Transactional
    public DeletedApplicantResponse archiveApplicant(User currentUser, Long applicantId) {
        Applicant applicant = getActiveApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);
        LocalDateTime archivedAt = LocalDateTime.now();
        applicant.setArchived(true);
        applicant.setArchivedAt(archivedAt);
        applicantRepository.save(applicant);
        return new DeletedApplicantResponse(applicant.getId(), true, archivedAt, "Applicant archived successfully");
    }

    @Transactional
    public BulkDeletedApplicantResponse archiveApplicantsBulk(User currentUser, BulkApplicantArchiveRequest request) {
        List<Long> archivedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (Long applicantId : request.getApplicantIds()) {
            try {
                Applicant applicant = getActiveApplicantOrThrow(applicantId);
                permissionService.validateApplicantAccess(currentUser, applicant);
                applicant.setArchived(true);
                applicant.setArchivedAt(LocalDateTime.now());
                applicantRepository.save(applicant);
                archivedIds.add(applicantId);
            } catch (Exception ex) {
                failedIds.add(applicantId);
            }
        }

        String message = failedIds.isEmpty()
            ? "Applicants archived successfully"
            : "Partial archive completed with some failures";
        return new BulkDeletedApplicantResponse(archivedIds.size(), archivedIds, failedIds, message);
    }

    @Transactional
    public RecoveredApplicantResponse recoverApplicant(User currentUser, Long applicantId) {
        Applicant applicant = getArchivedApplicantOrThrow(applicantId);
        permissionService.validateApplicantAccess(currentUser, applicant);

        if (!applicant.getFacility().isActive()) {
            throw new IllegalArgumentException("Cannot recover applicant because facility is inactive");
        }

        if (applicantRepository.existsByFacilityIdAndEmailIgnoreCaseAndIdNotAndIsArchivedFalse(
            applicant.getFacility().getId(), applicant.getEmail(), applicant.getId()
        )) {
            throw new IllegalArgumentException("Cannot recover applicant due to duplicate email in facility");
        }

        applicant.setArchived(false);
        applicant.setArchivedAt(null);
        applicantRepository.save(applicant);
        return new RecoveredApplicantResponse(applicant.getId(), false, LocalDateTime.now(), "Applicant recovered successfully");
    }

    @Transactional
    public BulkRecoveredApplicantResponse recoverApplicantsBulk(User currentUser, BulkApplicantArchiveRequest request) {
        List<Long> recoveredIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (Long applicantId : request.getApplicantIds()) {
            try {
                Applicant applicant = getArchivedApplicantOrThrow(applicantId);
                permissionService.validateApplicantAccess(currentUser, applicant);

                if (!applicant.getFacility().isActive()) {
                    throw new IllegalArgumentException("Facility is inactive");
                }

                if (applicantRepository.existsByFacilityIdAndEmailIgnoreCaseAndIdNotAndIsArchivedFalse(
                    applicant.getFacility().getId(), applicant.getEmail(), applicant.getId()
                )) {
                    throw new IllegalArgumentException("Duplicate email exists");
                }

                applicant.setArchived(false);
                applicant.setArchivedAt(null);
                applicantRepository.save(applicant);
                recoveredIds.add(applicantId);
            } catch (Exception ex) {
                failedIds.add(applicantId);
            }
        }

        String message = failedIds.isEmpty()
            ? "Applicants recovered successfully"
            : "Partial recovery completed with some failures";
        return new BulkRecoveredApplicantResponse(recoveredIds.size(), recoveredIds, failedIds, message);
    }

    public int archiveApplicantsByFacilityId(Long facilityId) {
        List<Applicant> applicants = applicantRepository.findByFacilityIdAndIsArchivedFalse(facilityId);
        LocalDateTime archivedAt = LocalDateTime.now();
        for (Applicant applicant : applicants) {
            applicant.setArchived(true);
            applicant.setArchivedAt(archivedAt);
        }
        applicantRepository.saveAll(applicants);
        return applicants.size();
    }

    public int recoverApplicantsByFacilityId(Long facilityId) {
        List<Applicant> applicants = applicantRepository.findByFacilityIdAndIsArchivedTrue(facilityId);
        int recovered = 0;
        for (Applicant applicant : applicants) {
            if (applicantRepository.existsByFacilityIdAndEmailIgnoreCaseAndIdNotAndIsArchivedFalse(
                facilityId, applicant.getEmail(), applicant.getId()
            )) {
                continue;
            }
            applicant.setArchived(false);
            applicant.setArchivedAt(null);
            recovered++;
        }
        applicantRepository.saveAll(applicants);
        return recovered;
    }

    private Applicant getActiveApplicantOrThrow(Long applicantId) {
        return applicantRepository.findByIdAndIsArchivedFalse(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Applicant not found"));
    }

    private Applicant getArchivedApplicantOrThrow(Long applicantId) {
        return applicantRepository.findByIdAndIsArchivedTrue(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Archived applicant not found"));
    }

    private ApplicantResponse toResponse(Applicant applicant, boolean includeInterviews, boolean includeOrientation) {
        List<ApplicantInterviewResponse> interviews = List.of();
        if (includeInterviews) {
            interviews = interviewRepository.findByApplicantIdOrderByCreatedAtDesc(applicant.getId())
                .stream()
                .map(ApplicantInterviewResponse::new)
                .toList();
        }

        ApplicantOrientationResponse orientation = null;
        if (includeOrientation) {
            orientation = orientationRepository.findByApplicantId(applicant.getId())
                .map(ApplicantOrientationResponse::new)
                .orElse(null);
        }

        return new ApplicantResponse(applicant, interviews, orientation);
    }

    private void ensureFacilityScoping(User currentUser, Long facilityId) {
        if (!permissionService.isAdmin(currentUser)) {
            if (facilityId == null) {
                throw new IllegalArgumentException("facilityId is required for non-admin users");
            }
            permissionService.validateFacilityAccess(currentUser, facilityId);
        } else if (facilityId != null) {
            permissionService.validateFacilityAccess(currentUser, facilityId);
        }
    }

    private Specification<Applicant> buildFilterSpec(
        Long facilityId,
        ApplicantStatus status,
        String role,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        Specification<Applicant> specification = Specification.where((root, query, cb) ->
            cb.isFalse(root.get("isArchived")));

        if (facilityId != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("facility").get("id"), facilityId));
        }

        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (StringUtils.hasText(role)) {
            specification = specification.and((root, query, cb) ->
                cb.equal(cb.lower(root.get("role")), role.trim().toLowerCase(Locale.ROOT)));
        }

        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay().minusNanos(1);

        if (fromDateTime != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("addedDate"), fromDateTime));
        }

        if (toDateTime != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("addedDate"), toDateTime));
        }

        return specification;
    }

    private Sort parseSort(String sort) {
        String rawSort = StringUtils.hasText(sort) ? sort.trim() : "addedDate,desc";
        String[] tokens = rawSort.split(",");
        String sortField = tokens.length > 0 && StringUtils.hasText(tokens[0]) ? tokens[0].trim() : "addedDate";
        
        // Validate sort field - only allow safe, known fields to prevent injection issues
        Set<String> allowedFields = Set.of("id", "name", "email", "role", "status", "addedDate", "updatedDate", "hiredAt");
        if (!allowedFields.contains(sortField)) {
            sortField = "addedDate"; // fallback to safe default
        }
        
        Sort.Direction direction = Sort.Direction.DESC;
        if (tokens.length > 1) {
            direction = "asc".equalsIgnoreCase(tokens[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        }
        return Sort.by(direction, sortField);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String requireTrimmed(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizeDuration(Integer durationMinutes, int defaultValue) {
        int normalized = durationMinutes == null ? defaultValue : durationMinutes;
        if (normalized <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }
        return normalized;
    }

    private InterviewType parseInterviewType(String interviewType) {
        if (!StringUtils.hasText(interviewType)) {
            throw new IllegalArgumentException("Interview type is required");
        }

        String normalized = interviewType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return InterviewType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Interview type must be one of: phone, video, in-person");
        }
    }

    private void validateStatusTransition(ApplicantStatus current, ApplicantStatus next) {
        // Transition guards intentionally disabled: any status can transition to any other status.
    }

    private double percentage(long part, long total) {
        return round2((part * 100.0) / total);
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record ResumeDownload(Resource resource, String contentType, String filename) { }
}
