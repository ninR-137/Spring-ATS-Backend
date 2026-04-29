package com.example.demo.responses;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarEventsResponse {

    private Long facilityId;
    private String facilityName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String view;
    private List<CalendarEventDto> events;
    private CalendarSummaryDto summary;

    // For view-specific responses
    private Map<String, List<CalendarEventDto>> eventsByDate;
    private List<WeekDayDto> weekDays;
    private String date;
    private List<TimeSlotDto> timeSlots;

    @Getter
    @Setter
    public static class CalendarSummaryDto {
        private int totalInterviews;
        private int scheduled;
        private int completed;
        private int noShow;
        private Map<String, Integer> byType;
        private Map<String, Integer> byRole;
    }

    @Getter
    @Setter
    public static class WeekDayDto {
        private String date;
        private String dayName;
        private List<CalendarEventDto> events;
    }

    @Getter
    @Setter
    public static class TimeSlotDto {
        private String time;
        private List<CalendarEventDto> events;
    }
}
