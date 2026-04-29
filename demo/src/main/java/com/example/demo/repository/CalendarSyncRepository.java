package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.CalendarSync;

public interface CalendarSyncRepository extends JpaRepository<CalendarSync, Long> {

    Optional<CalendarSync> findByFacilityIdAndCalendarType(Long facilityId, String calendarType);
}
