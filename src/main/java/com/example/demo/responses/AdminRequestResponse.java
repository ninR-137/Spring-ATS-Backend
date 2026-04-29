package com.example.demo.responses;

import com.example.demo.model.AdminRequest;
import com.example.demo.model.AdminRequestStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminRequestResponse {
    private final Long id;
    private final Long userId;
    private final String username;
    private final String email;
    private final AdminRequestStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime reviewedAt;

    public AdminRequestResponse(AdminRequest adminRequest) {
        this.id = adminRequest.getId();
        this.userId = adminRequest.getUser().getId();
        this.username = adminRequest.getUser().getUsername();
        this.email = adminRequest.getUser().getEmail();
        this.status = adminRequest.getStatus();
        this.createdAt = adminRequest.getCreatedAt();
        this.reviewedAt = adminRequest.getReviewedAt();
    }
}