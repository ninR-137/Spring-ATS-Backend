package com.example.demo.responses;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeletedApplicantResponse {

    private Long applicantId;
    private boolean archived;
    private LocalDateTime archivedAt;
    private String message;
}
