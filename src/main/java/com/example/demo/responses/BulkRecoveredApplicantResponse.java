package com.example.demo.responses;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BulkRecoveredApplicantResponse {

    private int recoveredCount;
    private List<Long> recoveredIds;
    private List<Long> failedIds;
    private String message;
}
