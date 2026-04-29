package com.example.demo.responses;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpcomingOrientationsResponse {

    private Long facilityId;
    private String facilityName;
    private int periodDays;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<CalendarEventDto> events;
    private int totalUpcoming;
}
