package com.example.demo.responses;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BulkStatusUpdateResponse {
    private int updatedCount;
    private List<Long> failedIds;
    private String message;
}
