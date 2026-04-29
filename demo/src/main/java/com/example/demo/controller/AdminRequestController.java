package com.example.demo.controller;

import com.example.demo.dto.AdminRequestReviewDto;
import com.example.demo.model.User;
import com.example.demo.responses.AdminRequestResponse;
import com.example.demo.service.AdminRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-requests")
public class AdminRequestController {
    private final AdminRequestService adminRequestService;

    public AdminRequestController(AdminRequestService adminRequestService) {
        this.adminRequestService = adminRequestService;
    }

    @PostMapping
    public ResponseEntity<AdminRequestResponse> createRequest(@AuthenticationPrincipal User currentUser) {
        AdminRequestResponse response = new AdminRequestResponse(adminRequestService.createRequest(currentUser));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AdminRequestResponse>> getMyRequests(@AuthenticationPrincipal User currentUser) {
        List<AdminRequestResponse> responses = adminRequestService.getMyRequests(currentUser)
                .stream()
                .map(AdminRequestResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminRequestResponse>> getPendingRequests() {
        List<AdminRequestResponse> responses = adminRequestService.getPendingRequests()
                .stream()
                .map(AdminRequestResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<AdminRequestResponse> reviewRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody AdminRequestReviewDto reviewDto
    ) {
        AdminRequestResponse response = new AdminRequestResponse(
                adminRequestService.reviewRequest(requestId, reviewDto.getStatus())
        );
        return ResponseEntity.ok(response);
    }
}