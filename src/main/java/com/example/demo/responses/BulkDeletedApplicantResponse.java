package com.example.demo.responses;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BulkDeletedApplicantResponse {

    private int archivedCount;
    private List<Long> archivedIds;
    private List<Long> failedIds;
    private String message;
}
