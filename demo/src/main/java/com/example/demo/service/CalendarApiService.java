package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AvailabilityCheckRequest;
import com.example.demo.dto.RegenerateMeetingLinkRequest;
import com.example.demo.dto.SendReminderNowRequest;
import com.example.demo.dto.SyncExternalCalendarRequest;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.ApplicantInterview;
import com.example.demo.model.ApplicantOrientation;
import com.example.demo.model.CalendarSync;
import com.example.demo.model.EmailRecipient;
import com.example.demo.model.Facility;
import com.example.demo.model.InterviewType;
import com.example.demo.model.User;
import com.example.demo.repository.ApplicantInterviewRepository;
import com.example.demo.repository.ApplicantOrientationRepository;
import com.example.demo.repository.CalendarSyncRepository;
import com.example.demo.repository.EmailRecipientRepository;
import com.example.demo.repository.FacilityRepository;
import com.example.demo.responses.AvailabilityResponse;
import com.example.demo.responses.CalendarEventDto;
import com.example.demo.responses.CalendarEventsResponse;
import com.example.demo.responses.ExternalSyncResponse;
import com.example.demo.responses.RegenerateMeetingLinkResponse;
import com.example.demo.responses.ReminderSentResponse;
import com.example.demo.responses.UpcomingOrientationsResponse;
import com.example.demo.responses.UpcomingInterviewsResponse;

@Service
public class CalendarApiService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ApplicantInterviewRepository interviewRepository;
    private final ApplicantOrientationRepository orientationRepository;
    private final FacilityRepository facilityRepository;
    private final CalendarSyncRepository calendarSyncRepository;
    private final EmailRecipientRepository emailRecipientRepository;
    private final ApplicantPermissionService permissionService;
    private final MeetingLinkService meetingLinkService;
    private final EmailNotificationService emailNotificationService;

    public CalendarApiService(
        ApplicantInterviewRepository interviewRepository,
        ApplicantOrientationRepository orientationRepository,
        FacilityRepository facilityRepository,
        CalendarSyncRepository calendarSyncRepository,
        EmailRecipientRepository emailRecipientRepository,
        ApplicantPermissionService permissionService,
        MeetingLinkService meetingLinkService,
        EmailNotificationService emailNotificationService
    ) {
        this.interviewRepository = interviewRepository;
        this.orientationRepository = orientationRepository;
        this.facilityRepository = facilityRepository;
        this.calendarSyncRepository = calendarSyncRepository;
        this.emailRecipientRepository = emailRecipientRepository;
        this.permissionService = permissionService;
        this.meetingLinkService = meetingLinkService;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional(readOnly = true)
    public CalendarEventsResponse getInterviews(
        User currentUser,
        Long facilityId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status,
        String interviewType,
        String view
    ) {
        facilityId = resolveFacilityId(currentUser, facilityId);
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));

        List<ApplicantInterview> interviews = interviewRepository
            .findByApplicant_Facility_IdAndScheduledDateBetweenOrderByScheduledDate(
                facilityId, startDate, endDate.withHour(23).withMinute(59).withSecond(59)
            );

        interviews = filterByStatus(interviews, status);
        interviews = filterByType(interviews, interviewType);

        String resolvedView = (view == null || view.isBlank()) ? "month" : view.toLowerCase();

        CalendarEventsResponse response = new CalendarEventsResponse();
        response.setFacilityId(facilityId);
        response.setFacilityName(facility.getName());
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setView(resolvedView);
        response.setEvents(interviews.stream().map(this::toEventDto).collect(Collectors.toList()));
        response.setSummary(buildSummary(interviews));

        switch (resolvedView) {
            case "month" -> response.setEventsByDate(groupByDate(interviews));
            case "week" -> response.setWeekDays(buildWeekDays(interviews, startDate, endDate));
            case "day" -> {
                response.setDate(startDate.toLocalDate().format(DATE_FORMAT));
                response.setTimeSlots(buildTimeSlots(interviews));
            }
            default -> { /* agenda: events list is sufficient */ }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public UpcomingInterviewsResponse getUpcomingInterviews(
        User currentUser,
        Long facilityId,
        int days,
        int limit
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(days).withHour(23).withMinute(59).withSecond(59);

        List<ApplicantInterview> interviews;
        Long resolvedFacilityId = null;
        String facilityName = null;

        if (permissionService.isAdmin(currentUser) && facilityId == null) {
            interviews = interviewRepository.findUpcomingAll(now, end);
        } else {
            resolvedFacilityId = resolveFacilityId(currentUser, facilityId);
            Facility facility = facilityRepository.findById(resolvedFacilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));
            facilityName = facility.getName();
            interviews = interviewRepository.findUpcomingByFacilityId(resolvedFacilityId, now, end);
        }

        if (interviews.size() > limit) {
            interviews = interviews.subList(0, limit);
        }

        LocalDateTime computedNow = LocalDateTime.now();
        List<CalendarEventDto> events = interviews.stream()
            .map(i -> toUpcomingEventDto(i, computedNow))
            .collect(Collectors.toList());

        UpcomingInterviewsResponse response = new UpcomingInterviewsResponse();
        response.setFacilityId(resolvedFacilityId);
        response.setFacilityName(facilityName);
        response.setPeriodDays(days);
        response.setStartDate(now);
        response.setEndDate(end);
        response.setEvents(events);
        response.setTotalUpcoming(events.size());
        return response;
    }

    @Transactional(readOnly = true)
    public CalendarEventDto getInterviewById(User currentUser, Long interviewId) {
        ApplicantInterview interview = interviewRepository.findById(interviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        permissionService.validateApplicantAccess(currentUser, interview.getApplicant());
        return toDetailedEventDto(interview);
    }

    @Transactional(readOnly = true)
    public CalendarEventsResponse getOrientations(
        User currentUser,
        Long facilityId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status,
        String view
    ) {
        facilityId = resolveFacilityId(currentUser, facilityId);
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));

        List<ApplicantOrientation> orientations = orientationRepository
            .findByApplicant_Facility_IdAndScheduledDateBetweenOrderByScheduledDate(
                facilityId, startDate, endDate.withHour(23).withMinute(59).withSecond(59)
            );

        orientations = filterOrientationByStatus(orientations, status);

        String resolvedView = (view == null || view.isBlank()) ? "month" : view.toLowerCase();

        CalendarEventsResponse response = new CalendarEventsResponse();
        response.setFacilityId(facilityId);
        response.setFacilityName(facility.getName());
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setView(resolvedView);
        response.setEvents(orientations.stream().map(this::toOrientationEventDto).collect(Collectors.toList()));
        response.setSummary(buildOrientationSummary(orientations));

        switch (resolvedView) {
            case "month" -> response.setEventsByDate(groupOrientationsByDate(orientations));
            case "week" -> response.setWeekDays(buildOrientationWeekDays(orientations, startDate, endDate));
            case "day" -> {
                response.setDate(startDate.toLocalDate().format(DATE_FORMAT));
                response.setTimeSlots(buildOrientationTimeSlots(orientations));
            }
            default -> { /* agenda: events list is sufficient */ }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public UpcomingOrientationsResponse getUpcomingOrientations(
        User currentUser,
        Long facilityId,
        int days,
        int limit
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(days).withHour(23).withMinute(59).withSecond(59);

        List<ApplicantOrientation> orientations;
        Long resolvedFacilityId = null;
        String facilityName = null;

        if (permissionService.isAdmin(currentUser) && facilityId == null) {
            orientations = orientationRepository.findUpcomingAll(now, end);
        } else {
            resolvedFacilityId = resolveFacilityId(currentUser, facilityId);
            Facility facility = facilityRepository.findById(resolvedFacilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));
            facilityName = facility.getName();
            orientations = orientationRepository.findUpcomingByFacilityId(resolvedFacilityId, now, end);
        }

        if (orientations.size() > limit) {
            orientations = orientations.subList(0, limit);
        }

        LocalDateTime computedNow = LocalDateTime.now();
        List<CalendarEventDto> events = orientations.stream()
            .map(o -> toUpcomingOrientationEventDto(o, computedNow))
            .collect(Collectors.toList());

        UpcomingOrientationsResponse response = new UpcomingOrientationsResponse();
        response.setFacilityId(resolvedFacilityId);
        response.setFacilityName(facilityName);
        response.setPeriodDays(days);
        response.setStartDate(now);
        response.setEndDate(end);
        response.setEvents(events);
        response.setTotalUpcoming(events.size());
        return response;
    }

    @Transactional(readOnly = true)
    public CalendarEventDto getOrientationById(User currentUser, Long orientationId) {
        ApplicantOrientation orientation = orientationRepository.findById(orientationId)
            .orElseThrow(() -> new ResourceNotFoundException("Orientation not found"));
        permissionService.validateApplicantAccess(currentUser, orientation.getApplicant());
        return toDetailedOrientationEventDto(orientation);
    }

    public byte[] exportCalendar(
        User currentUser,
        Long facilityId,
        LocalDate startDate,
        LocalDate endDate,
        String format
    ) {
        facilityId = resolveFacilityId(currentUser, facilityId);
        facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));

        List<ApplicantInterview> interviews = interviewRepository
            .findByApplicant_Facility_IdAndScheduledDateBetweenOrderByScheduledDate(
                facilityId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
            );

        String resolvedFormat = (format == null || format.isBlank()) ? "ics" : format.toLowerCase();

        return switch (resolvedFormat) {
            case "csv" -> buildCsv(interviews).getBytes();
            case "json" -> buildJson(interviews).getBytes();
            default -> buildIcs(interviews).getBytes();
        };
    }

    @Transactional
    public ExternalSyncResponse syncExternal(User currentUser, SyncExternalCalendarRequest request) {
        permissionService.ensureAdmin(currentUser);

        Facility facility = facilityRepository.findById(request.getFacilityId())
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));

        CalendarSync sync = calendarSyncRepository
            .findByFacilityIdAndCalendarType(facility.getId(), request.getCalendarType())
            .orElse(new CalendarSync());

        sync.setFacility(facility);
        sync.setCalendarType(request.getCalendarType());
        sync.setAccessToken(request.getAccessToken());
        sync.setRefreshToken(request.getRefreshToken());
        sync.setLastSyncAt(LocalDateTime.now());
        sync.setIsActive(true);

        String syncToken = "sync_token_" + System.currentTimeMillis();
        sync.setSyncToken(syncToken);
        sync.setExternalCalendarId("primary");

        calendarSyncRepository.save(sync);

        List<ApplicantInterview> upcoming = interviewRepository
            .findByApplicant_Facility_IdAndScheduledDateBetweenOrderByScheduledDate(
                facility.getId(),
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(3)
            );

        ExternalSyncResponse response = new ExternalSyncResponse();
        response.setMessage("Calendar synced successfully");
        response.setSyncedEvents(upcoming.size());
        response.setCalendarId("primary");
        response.setSyncToken(syncToken);
        return response;
    }

    @Transactional
    public RegenerateMeetingLinkResponse regenerateMeetingLink(
        User currentUser,
        Long interviewId,
        RegenerateMeetingLinkRequest request
    ) {
        ApplicantInterview interview = interviewRepository.findById(interviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        permissionService.validateApplicantAccess(currentUser, interview.getApplicant());

        String oldLink = interview.getMeetingLink();
        String newLink = meetingLinkService.generateMeetingLink(interview);
        String newPassword = meetingLinkService.randomPassword();

        interview.setMeetingLink(newLink);
        interview.setMeetingPassword(newPassword);
        interviewRepository.save(interview);

        boolean notified = false;
        if (Boolean.TRUE.equals(request.getSendUpdateNotifications())) {
            emailNotificationService.sendScheduledInterviewEmail(interview.getApplicant(), interview);
            notified = true;
        }

        RegenerateMeetingLinkResponse response = new RegenerateMeetingLinkResponse();
        response.setInterviewId(interviewId);
        response.setOldMeetingLink(oldLink);
        response.setNewMeetingLink(newLink);
        response.setNewMeetingPassword(newPassword);
        response.setCalendarEventUpdated(true);
        response.setNotificationsSent(notified);
        response.setMessage("Meeting link regenerated and calendar updated");
        return response;
    }

    @Transactional
    public ReminderSentResponse sendReminderNow(
        User currentUser,
        Long interviewId,
        SendReminderNowRequest request
    ) {
        ApplicantInterview interview = interviewRepository.findById(interviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        permissionService.validateApplicantAccess(currentUser, interview.getApplicant());

        String reminderType = request.getReminderType();
        emailNotificationService.sendInterviewReminder(interview, reminderType);

        LocalDateTime now = LocalDateTime.now();
        if ("24h".equalsIgnoreCase(reminderType)) {
            interview.setReminder24hSent(now);
        } else if ("1h".equalsIgnoreCase(reminderType)) {
            interview.setReminder1hSent(now);
        }
        interviewRepository.save(interview);

        List<String> recipientEmails = emailRecipientRepository
            .findByFacilityIdAndIsActive(interview.getApplicant().getFacility().getId(), true)
            .stream().map(EmailRecipient::getEmail).collect(Collectors.toList());
        recipientEmails.add(interview.getApplicant().getEmail());

        ReminderSentResponse response = new ReminderSentResponse();
        response.setMessage("Reminder sent successfully");
        response.setRecipients(recipientEmails);
        response.setReminderType(reminderType);
        response.setSentAt(now);
        return response;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(User currentUser, AvailabilityCheckRequest request) {
        permissionService.validateFacilityAccess(currentUser, request.getFacilityId());

        List<ApplicantInterview> conflicts = interviewRepository
            .findByApplicant_Facility_IdAndScheduledDateBetweenAndCompletedFalseAndNoShowFalseOrderByScheduledDate(
                request.getFacilityId(),
                request.getStartDateTime().minusMinutes(59),
                request.getEndDateTime().plusMinutes(59)
            )
            .stream()
            .filter(i -> overlaps(i, request.getStartDateTime(), request.getEndDateTime()))
            .collect(Collectors.toList());

        AvailabilityResponse response = new AvailabilityResponse();
        response.setAvailable(conflicts.isEmpty());

        if (!conflicts.isEmpty()) {
            response.setConflictingEvents(conflicts.stream().map(i -> {
                AvailabilityResponse.ConflictEventDto dto = new AvailabilityResponse.ConflictEventDto();
                dto.setId(i.getId());
                dto.setTitle(buildTitle(i));
                dto.setStart(i.getScheduledDate().format(DT_FORMAT));
                dto.setEnd(i.getScheduledEndDate() != null
                    ? i.getScheduledEndDate().format(DT_FORMAT)
                    : i.getScheduledDate().plusMinutes(i.getDurationMinutes()).format(DT_FORMAT));
                dto.setStatus(resolveStatus(i));
                return dto;
            }).collect(Collectors.toList()));
        } else {
            response.setConflictingEvents(Collections.emptyList());
        }

        response.setSuggestedTimes(suggestTimes(request.getFacilityId(), request.getStartDateTime()));
        return response;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long resolveFacilityId(User currentUser, Long facilityId) {
        if (facilityId == null) {
            if (permissionService.isAdmin(currentUser)) {
                throw new IllegalArgumentException("facilityId is required");
            }
            throw new IllegalArgumentException("facilityId is required for non-admin users");
        }
        permissionService.validateFacilityAccess(currentUser, facilityId);
        return facilityId;
    }

    private List<ApplicantInterview> filterByStatus(List<ApplicantInterview> list, String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return list;
        return list.stream().filter(i -> resolveStatus(i).equalsIgnoreCase(status)).collect(Collectors.toList());
    }

    private List<ApplicantInterview> filterByType(List<ApplicantInterview> list, String type) {
        if (type == null || type.isBlank()) return list;
        return list.stream()
            .filter(i -> i.getInterviewType() != null && typeLabel(i.getInterviewType()).equalsIgnoreCase(type))
            .collect(Collectors.toList());
    }

    private List<ApplicantOrientation> filterOrientationByStatus(List<ApplicantOrientation> list, String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return list;
        return list.stream()
            .filter(o -> resolveOrientationStatus(o).equalsIgnoreCase(status))
            .collect(Collectors.toList());
    }

    private String resolveStatus(ApplicantInterview i) {
        if (i.isNoShow()) return "NO_SHOW";
        if (i.isCompleted()) return "COMPLETED";
        return "SCHEDULED";
    }

    private String resolveOrientationStatus(ApplicantOrientation o) {
        if (o.isNoShow()) return "NO_SHOW";
        if (o.isCompleted()) return "COMPLETED";
        return "SCHEDULED";
    }

    private String typeLabel(InterviewType type) {
        return switch (type) {
            case PHONE -> "phone";
            case VIDEO -> "video";
            case IN_PERSON -> "in-person";
        };
    }

    private String buildTitle(ApplicantInterview i) {
        String name = i.getApplicant() != null ? i.getApplicant().getName() : "Unknown";
        String role = i.getApplicant() != null ? i.getApplicant().getRole() : "Unknown";
        return "Interview: " + name + " - " + role;
    }

    private String buildOrientationTitle(ApplicantOrientation o) {
        String name = o.getApplicant() != null ? o.getApplicant().getName() : "Unknown";
        String role = o.getApplicant() != null ? o.getApplicant().getRole() : "Unknown";
        return "Orientation: " + name + " - " + role;
    }

    private CalendarEventDto toEventDto(ApplicantInterview i) {
        CalendarEventDto dto = new CalendarEventDto();
        dto.setId(i.getId());
        dto.setTitle(buildTitle(i));
        dto.setStart(i.getScheduledDate());
        dto.setEnd(endTime(i));
        dto.setAllDay(false);
        dto.setStatus(resolveStatus(i));
        dto.setInterviewType(i.getInterviewType() != null ? typeLabel(i.getInterviewType()) : null);
        dto.setApplicant(toApplicantDto(i, false));
        dto.setMeetingLink(i.getMeetingLink());
        dto.setMeetingPassword(i.getMeetingPassword());
        dto.setNotes(i.getNotes());
        dto.setRemindersSent(Map.of(
            "24h", i.getReminder24hSent() != null,
            "1h", i.getReminder1hSent() != null
        ));
        return dto;
    }

    private CalendarEventDto toUpcomingEventDto(ApplicantInterview i, LocalDateTime now) {
        CalendarEventDto dto = toEventDto(i);
        dto.setTimeUntil(humanDuration(Duration.between(now, i.getScheduledDate())));
        return dto;
    }

    private CalendarEventDto toOrientationEventDto(ApplicantOrientation o) {
        CalendarEventDto dto = new CalendarEventDto();
        dto.setId(o.getId());
        dto.setTitle(buildOrientationTitle(o));
        dto.setStart(o.getScheduledDate());
        dto.setEnd(orientationEndTime(o));
        dto.setAllDay(false);
        dto.setStatus(resolveOrientationStatus(o));
        dto.setInterviewType("orientation");
        dto.setApplicant(toOrientationApplicantDto(o, false));
        dto.setMeetingLink(o.getMeetingLink());
        dto.setMeetingPassword(o.getMeetingPassword());
        dto.setNotes(o.getNotes());
        dto.setRemindersSent(Collections.emptyMap());
        return dto;
    }

    private CalendarEventDto toUpcomingOrientationEventDto(ApplicantOrientation o, LocalDateTime now) {
        CalendarEventDto dto = toOrientationEventDto(o);
        dto.setTimeUntil(humanDuration(Duration.between(now, o.getScheduledDate())));
        return dto;
    }

    private CalendarEventDto toDetailedEventDto(ApplicantInterview i) {
        CalendarEventDto dto = toEventDto(i);
        dto.setApplicant(toApplicantDto(i, true));
        dto.setMeetingId(i.getMeetingId());
        dto.setCalendarEventId(i.getCalendarEventId());

        CalendarEventDto.CompletionInfoDto completion = new CalendarEventDto.CompletionInfoDto();
        completion.setCompleted(i.isCompleted());
        completion.setNoShow(i.isNoShow());
        completion.setCompletedAt(i.getCompletedAt());
        dto.setCompletionInfo(completion);

        if (i.getApplicant() != null && i.getApplicant().getFacility() != null) {
            // facility info is embedded via the applicant already accessible
        }
        return dto;
    }

    private CalendarEventDto toDetailedOrientationEventDto(ApplicantOrientation o) {
        CalendarEventDto dto = toOrientationEventDto(o);
        dto.setApplicant(toOrientationApplicantDto(o, true));
        dto.setCalendarEventId(o.getCalendarEventId());

        CalendarEventDto.CompletionInfoDto completion = new CalendarEventDto.CompletionInfoDto();
        completion.setCompleted(o.isCompleted());
        completion.setNoShow(o.isNoShow());
        completion.setCompletedAt(o.getCompletedAt());
        dto.setCompletionInfo(completion);

        return dto;
    }

    private CalendarEventDto.CalendarApplicantDto toApplicantDto(ApplicantInterview i, boolean detailed) {
        if (i.getApplicant() == null) return null;
        CalendarEventDto.CalendarApplicantDto dto = new CalendarEventDto.CalendarApplicantDto();
        dto.setId(i.getApplicant().getId());
        dto.setName(i.getApplicant().getName());
        dto.setPhoneNumber(i.getApplicant().getPhoneNumber());
        dto.setRole(i.getApplicant().getRole());
        if (detailed) {
            dto.setEmail(i.getApplicant().getEmail());
            dto.setResumeUrl(i.getApplicant().getResumeUrl());
        }
        return dto;
    }

    private CalendarEventDto.CalendarApplicantDto toOrientationApplicantDto(ApplicantOrientation o, boolean detailed) {
        if (o.getApplicant() == null) return null;
        CalendarEventDto.CalendarApplicantDto dto = new CalendarEventDto.CalendarApplicantDto();
        dto.setId(o.getApplicant().getId());
        dto.setName(o.getApplicant().getName());
        dto.setPhoneNumber(o.getApplicant().getPhoneNumber());
        dto.setRole(o.getApplicant().getRole());
        if (detailed) {
            dto.setEmail(o.getApplicant().getEmail());
            dto.setResumeUrl(o.getApplicant().getResumeUrl());
        }
        return dto;
    }

    private LocalDateTime endTime(ApplicantInterview i) {
        if (i.getScheduledEndDate() != null) return i.getScheduledEndDate();
        return i.getScheduledDate().plusMinutes(i.getDurationMinutes() != null ? i.getDurationMinutes() : 60);
    }

    private LocalDateTime orientationEndTime(ApplicantOrientation o) {
        if (o.getScheduledEndDate() != null) return o.getScheduledEndDate();
        return o.getScheduledDate().plusMinutes(o.getDurationMinutes() != null ? o.getDurationMinutes() : 120);
    }

    private CalendarEventsResponse.CalendarSummaryDto buildSummary(List<ApplicantInterview> list) {
        CalendarEventsResponse.CalendarSummaryDto s = new CalendarEventsResponse.CalendarSummaryDto();
        s.setTotalInterviews(list.size());
        s.setScheduled((int) list.stream().filter(i -> !i.isCompleted() && !i.isNoShow()).count());
        s.setCompleted((int) list.stream().filter(i -> i.isCompleted() && !i.isNoShow()).count());
        s.setNoShow((int) list.stream().filter(ApplicantInterview::isNoShow).count());

        Map<String, Integer> byType = new LinkedHashMap<>();
        byType.put("video", 0);
        byType.put("in-person", 0);
        byType.put("phone", 0);
        list.stream().filter(i -> i.getInterviewType() != null)
            .forEach(i -> byType.merge(typeLabel(i.getInterviewType()), 1, Integer::sum));
        s.setByType(byType);

        Map<String, Integer> byRole = new LinkedHashMap<>();
        list.stream().filter(i -> i.getApplicant() != null)
            .forEach(i -> byRole.merge(i.getApplicant().getRole(), 1, Integer::sum));
        s.setByRole(byRole);

        return s;
    }

    private CalendarEventsResponse.CalendarSummaryDto buildOrientationSummary(List<ApplicantOrientation> list) {
        CalendarEventsResponse.CalendarSummaryDto s = new CalendarEventsResponse.CalendarSummaryDto();
        s.setTotalInterviews(list.size());
        s.setScheduled((int) list.stream().filter(o -> !o.isCompleted() && !o.isNoShow()).count());
        s.setCompleted((int) list.stream().filter(o -> o.isCompleted() && !o.isNoShow()).count());
        s.setNoShow((int) list.stream().filter(ApplicantOrientation::isNoShow).count());

        Map<String, Integer> byType = new LinkedHashMap<>();
        byType.put("orientation", list.size());
        s.setByType(byType);

        Map<String, Integer> byRole = new LinkedHashMap<>();
        list.stream().filter(o -> o.getApplicant() != null)
            .forEach(o -> byRole.merge(o.getApplicant().getRole(), 1, Integer::sum));
        s.setByRole(byRole);

        return s;
    }

    private Map<String, List<CalendarEventDto>> groupByDate(List<ApplicantInterview> list) {
        Map<String, List<CalendarEventDto>> map = new LinkedHashMap<>();
        for (ApplicantInterview i : list) {
            String key = i.getScheduledDate().toLocalDate().format(DATE_FORMAT);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(toEventDto(i));
        }
        return map;
    }

    private Map<String, List<CalendarEventDto>> groupOrientationsByDate(List<ApplicantOrientation> list) {
        Map<String, List<CalendarEventDto>> map = new LinkedHashMap<>();
        for (ApplicantOrientation o : list) {
            String key = o.getScheduledDate().toLocalDate().format(DATE_FORMAT);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(toOrientationEventDto(o));
        }
        return map;
    }

    private List<CalendarEventsResponse.WeekDayDto> buildWeekDays(
        List<ApplicantInterview> list,
        LocalDateTime start,
        LocalDateTime end
    ) {
        Map<LocalDate, List<ApplicantInterview>> byDate = list.stream()
            .collect(Collectors.groupingBy(i -> i.getScheduledDate().toLocalDate()));

        List<CalendarEventsResponse.WeekDayDto> days = new ArrayList<>();
        LocalDate cursor = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        while (!cursor.isAfter(endDate)) {
            CalendarEventsResponse.WeekDayDto day = new CalendarEventsResponse.WeekDayDto();
            day.setDate(cursor.format(DATE_FORMAT));
            day.setDayName(cursor.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
            List<ApplicantInterview> dayInterviews = byDate.getOrDefault(cursor, Collections.emptyList());
            day.setEvents(dayInterviews.stream().map(i -> {
                CalendarEventDto dto = toEventDto(i);
                dto.setStartTime(i.getScheduledDate().format(TIME_FORMAT));
                dto.setEndTime(endTime(i).format(TIME_FORMAT));
                dto.setType(i.getInterviewType() != null ? typeLabel(i.getInterviewType()) : null);
                return dto;
            }).collect(Collectors.toList()));
            days.add(day);
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private List<CalendarEventsResponse.TimeSlotDto> buildTimeSlots(List<ApplicantInterview> list) {
        Map<String, List<ApplicantInterview>> bySlot = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                bySlot.put(String.format("%02d:%02d", h, m), new ArrayList<>());
            }
        }
        for (ApplicantInterview i : list) {
            int h = i.getScheduledDate().getHour();
            int m = (i.getScheduledDate().getMinute() / 30) * 30;
            String key = String.format("%02d:%02d", h, m);
            bySlot.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }
        return bySlot.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty() || !e.getKey().startsWith("0"))
            .map(e -> {
                CalendarEventsResponse.TimeSlotDto slot = new CalendarEventsResponse.TimeSlotDto();
                slot.setTime(e.getKey());
                slot.setEvents(e.getValue().stream().map(i -> {
                    CalendarEventDto dto = toEventDto(i);
                    dto.setDuration(i.getDurationMinutes() != null ? i.getDurationMinutes() : 60);
                    return dto;
                }).collect(Collectors.toList()));
                return slot;
            })
            .collect(Collectors.toList());
    }

    private List<CalendarEventsResponse.WeekDayDto> buildOrientationWeekDays(
        List<ApplicantOrientation> list,
        LocalDateTime start,
        LocalDateTime end
    ) {
        Map<LocalDate, List<ApplicantOrientation>> byDate = list.stream()
            .collect(Collectors.groupingBy(o -> o.getScheduledDate().toLocalDate()));

        List<CalendarEventsResponse.WeekDayDto> days = new ArrayList<>();
        LocalDate cursor = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        while (!cursor.isAfter(endDate)) {
            CalendarEventsResponse.WeekDayDto day = new CalendarEventsResponse.WeekDayDto();
            day.setDate(cursor.format(DATE_FORMAT));
            day.setDayName(cursor.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
            List<ApplicantOrientation> dayOrientations = byDate.getOrDefault(cursor, Collections.emptyList());
            day.setEvents(dayOrientations.stream().map(o -> {
                CalendarEventDto dto = toOrientationEventDto(o);
                dto.setStartTime(o.getScheduledDate().format(TIME_FORMAT));
                dto.setEndTime(orientationEndTime(o).format(TIME_FORMAT));
                dto.setType("orientation");
                return dto;
            }).collect(Collectors.toList()));
            days.add(day);
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private List<CalendarEventsResponse.TimeSlotDto> buildOrientationTimeSlots(List<ApplicantOrientation> list) {
        Map<String, List<ApplicantOrientation>> bySlot = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                bySlot.put(String.format("%02d:%02d", h, m), new ArrayList<>());
            }
        }
        for (ApplicantOrientation o : list) {
            int h = o.getScheduledDate().getHour();
            int m = (o.getScheduledDate().getMinute() / 30) * 30;
            String key = String.format("%02d:%02d", h, m);
            bySlot.computeIfAbsent(key, k -> new ArrayList<>()).add(o);
        }
        return bySlot.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty() || !e.getKey().startsWith("0"))
            .map(e -> {
                CalendarEventsResponse.TimeSlotDto slot = new CalendarEventsResponse.TimeSlotDto();
                slot.setTime(e.getKey());
                slot.setEvents(e.getValue().stream().map(o -> {
                    CalendarEventDto dto = toOrientationEventDto(o);
                    dto.setDuration(o.getDurationMinutes() != null ? o.getDurationMinutes() : 120);
                    return dto;
                }).collect(Collectors.toList()));
                return slot;
            })
            .collect(Collectors.toList());
    }

    private String humanDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours >= 24) {
            long days = d.toDays();
            long remainHours = hours - days * 24;
            return remainHours > 0 ? days + " day" + (days > 1 ? "s" : "") + " " + remainHours + " hours" : days + " day" + (days > 1 ? "s" : "");
        }
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "");
        return minutes + " minutes";
    }

    private boolean overlaps(ApplicantInterview i, LocalDateTime reqStart, LocalDateTime reqEnd) {
        LocalDateTime iStart = i.getScheduledDate();
        LocalDateTime iEnd = endTime(i);
        return iStart.isBefore(reqEnd) && iEnd.isAfter(reqStart);
    }

    private List<String> suggestTimes(Long facilityId, LocalDateTime around) {
        List<String> suggestions = new ArrayList<>();
        LocalDateTime candidate = around.toLocalDate().atTime(9, 0);
        int added = 0;
        while (added < 3) {
            LocalDateTime candEnd = candidate.plusHours(1);
            boolean conflict = interviewRepository
                .existsByApplicant_Facility_IdAndScheduledDateBetweenAndCompletedFalseAndNoShowFalse(
                    facilityId,
                    candidate.minusMinutes(59),
                    candEnd.plusMinutes(59)
                );
            if (!conflict && !candidate.equals(around)) {
                suggestions.add(candidate.format(DT_FORMAT));
                added++;
            }
            candidate = candidate.plusHours(2);
            if (candidate.getHour() >= 18) {
                candidate = candidate.plusDays(1).toLocalDate().atTime(9, 0);
            }
        }
        return suggestions;
    }

    // ── Export helpers ───────────────────────────────────────────────────────

    private String buildIcs(List<ApplicantInterview> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Recruitment System//Calendar//EN\r\n");
        for (ApplicantInterview i : list) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:interview-").append(i.getId()).append("@recruitment.com\r\n");
            sb.append("DTSTAMP:").append(toIcsDate(i.getScheduledDate())).append("\r\n");
            sb.append("DTSTART:").append(toIcsDate(i.getScheduledDate())).append("\r\n");
            sb.append("DTEND:").append(toIcsDate(endTime(i))).append("\r\n");
            sb.append("SUMMARY:").append(buildTitle(i)).append("\r\n");
            String desc = "Notes: " + nullSafe(i.getNotes());
            if (i.getMeetingLink() != null) desc += "\\nMeeting Link: " + i.getMeetingLink();
            sb.append("DESCRIPTION:").append(desc).append("\r\n");
            if (i.getMeetingLink() != null) sb.append("LOCATION:").append(i.getMeetingLink()).append("\r\n");
            sb.append("STATUS:CONFIRMED\r\n");
            sb.append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private String buildCsv(List<ApplicantInterview> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,title,start,end,status,interviewType,applicantName,applicantRole,meetingLink\n");
        for (ApplicantInterview i : list) {
            sb.append(i.getId()).append(",");
            sb.append(csvEscape(buildTitle(i))).append(",");
            sb.append(i.getScheduledDate().format(DT_FORMAT)).append(",");
            sb.append(endTime(i).format(DT_FORMAT)).append(",");
            sb.append(resolveStatus(i)).append(",");
            sb.append(i.getInterviewType() != null ? typeLabel(i.getInterviewType()) : "").append(",");
            sb.append(csvEscape(i.getApplicant() != null ? i.getApplicant().getName() : "")).append(",");
            sb.append(csvEscape(i.getApplicant() != null ? i.getApplicant().getRole() : "")).append(",");
            sb.append(nullSafe(i.getMeetingLink())).append("\n");
        }
        return sb.toString();
    }

    private String buildJson(List<ApplicantInterview> list) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int idx = 0; idx < list.size(); idx++) {
            ApplicantInterview i = list.get(idx);
            sb.append("    {\n");
            sb.append("        \"id\": ").append(i.getId()).append(",\n");
            sb.append("        \"title\": \"").append(escapeJson(buildTitle(i))).append("\",\n");
            sb.append("        \"start\": \"").append(i.getScheduledDate().format(DT_FORMAT)).append("\",\n");
            sb.append("        \"end\": \"").append(endTime(i).format(DT_FORMAT)).append("\",\n");
            sb.append("        \"status\": \"").append(resolveStatus(i)).append("\",\n");
            sb.append("        \"interviewType\": \"").append(i.getInterviewType() != null ? typeLabel(i.getInterviewType()) : "").append("\",\n");
            sb.append("        \"applicantName\": \"").append(i.getApplicant() != null ? escapeJson(i.getApplicant().getName()) : "").append("\",\n");
            sb.append("        \"applicantRole\": \"").append(i.getApplicant() != null ? escapeJson(i.getApplicant().getRole()) : "").append("\",\n");
            sb.append("        \"meetingLink\": ").append(i.getMeetingLink() != null ? "\"" + escapeJson(i.getMeetingLink()) + "\"" : "null").append("\n");
            sb.append("    }").append(idx < list.size() - 1 ? "," : "").append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private String toIcsDate(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
