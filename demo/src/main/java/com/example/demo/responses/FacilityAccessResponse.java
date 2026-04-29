package com.example.demo.responses;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityAccessResponse {

    private Long id;
    private String name;
    private String location;
    private boolean isActive;

    // Admin-focused fields
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private Long userCount;
    private Long applicantCount;
    private Long emailRecipientCount;

    // Regular user-focused field (no join timestamp currently persisted)
    private LocalDateTime assignedAt;
}
