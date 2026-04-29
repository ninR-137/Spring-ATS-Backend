package com.example.demo.responses;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecoveredApplicantResponse {

    private Long applicantId;
    private boolean archived;
    private LocalDateTime recoveredAt;
    private String message;
}
