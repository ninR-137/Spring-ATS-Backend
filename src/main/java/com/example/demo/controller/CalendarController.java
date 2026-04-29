package com.example.demo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AvailabilityCheckRequest;
import com.example.demo.dto.RegenerateMeetingLinkRequest;
import com.example.demo.dto.SendReminderNowRequest;
import com.example.demo.dto.SyncExternalCalendarRequest;
import com.example.demo.model.User;
import com.example.demo.responses.AvailabilityResponse;
import com.example.demo.responses.CalendarEventDto;
import com.example.demo.responses.CalendarEventsResponse;
import com.example.demo.responses.ExternalSyncResponse;
import com.example.demo.responses.RegenerateMeetingLinkResponse;
import com.example.demo.responses.ReminderSentResponse;
import com.example.demo.responses.UpcomingOrientationsResponse;
import com.example.demo.responses.UpcomingInterviewsResponse;
import com.example.demo.service.CalendarApiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarApiService calendarApiService;

    public CalendarController(CalendarApiService calendarApiService) {
        this.calendarApiService = calendarApiService;
    }

    @GetMapping("/interviews")
    public ResponseEntity<CalendarEventsResponse> getInterviews(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Long facilityId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, pattern = "yyyy-MM-dd||yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, pattern = "yyyy-MM-dd||yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endDate,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String interviewType,
        @RequestParam(required = false, defaultValue = "month") String view
    ) {
        CalendarEventsResponse response = calendarApiService.getInterviews(
            currentUser, facilityId, startDate, endDate, status, interviewType, view
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/interviews/upcoming")
    public ResponseEntity<UpcomingInterviewsResponse> getUpcoming(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Long facilityId,
        @RequestParam(defaultValue = "7") int days,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(calendarApiService.getUpcomingInterviews(currentUser, facilityId, days, limit));
    }

    @GetMapping("/interviews/{interviewId}")
    public ResponseEntity<CalendarEventDto> getInterviewById(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long interviewId
    ) {
        return ResponseEntity.ok(calendarApiService.getInterviewById(currentUser, interviewId));
    }

    @GetMapping("/orientations")
    public ResponseEntity<CalendarEventsResponse> getOrientations(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Long facilityId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, pattern = "yyyy-MM-dd||yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, pattern = "yyyy-MM-dd||yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endDate,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "month") String view
    ) {
        CalendarEventsResponse response = calendarApiService.getOrientations(
            currentUser, facilityId, startDate, endDate, status, view
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orientations/upcoming")
    public ResponseEntity<UpcomingOrientationsResponse> getUpcomingOrientations(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Long facilityId,
        @RequestParam(defaultValue = "7") int days,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(calendarApiService.getUpcomingOrientations(currentUser, facilityId, days, limit));
    }

    @GetMapping("/orientations/{orientationId}")
    public ResponseEntity<CalendarEventDto> getOrientationById(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long orientationId
    ) {
        return ResponseEntity.ok(calendarApiService.getOrientationById(currentUser, orientationId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCalendar(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Long facilityId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false, defaultValue = "ics") String format
    ) {
        byte[] data = calendarApiService.exportCalendar(currentUser, facilityId, startDate, endDate, format);

        String resolvedFormat = (format == null || format.isBlank()) ? "ics" : format.toLowerCase();
        MediaType mediaType = switch (resolvedFormat) {
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "json" -> MediaType.APPLICATION_JSON;
            default -> MediaType.parseMediaType("text/calendar");
        };

        String filename = "interviews_" + startDate + "." + resolvedFormat;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentType(mediaType);

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @PostMapping("/sync-external")
    public ResponseEntity<ExternalSyncResponse> syncExternal(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody SyncExternalCalendarRequest request
    ) {
        return ResponseEntity.ok(calendarApiService.syncExternal(currentUser, request));
    }

    @PostMapping("/meeting-link/{interviewId}/regenerate")
    public ResponseEntity<RegenerateMeetingLinkResponse> regenerateMeetingLink(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long interviewId,
        @RequestBody(required = false) RegenerateMeetingLinkRequest request
    ) {
        if (request == null) request = new RegenerateMeetingLinkRequest();
        return ResponseEntity.ok(calendarApiService.regenerateMeetingLink(currentUser, interviewId, request));
    }

    @PostMapping("/reminders/send-now/{interviewId}")
    public ResponseEntity<ReminderSentResponse> sendReminderNow(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long interviewId,
        @Valid @RequestBody SendReminderNowRequest request
    ) {
        return ResponseEntity.ok(calendarApiService.sendReminderNow(currentUser, interviewId, request));
    }

    @PostMapping("/availability/check")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody AvailabilityCheckRequest request
    ) {
        return ResponseEntity.ok(calendarApiService.checkAvailability(currentUser, request));
    }
}
