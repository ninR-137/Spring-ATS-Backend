package com.example.demo.responses;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityResponse {

    private boolean available;
    private List<ConflictEventDto> conflictingEvents;
    private List<String> suggestedTimes;

    @Getter
    @Setter
    public static class ConflictEventDto {
        private Long id;
        private String title;
        private String start;
        private String end;
        private String status;
    }
}
