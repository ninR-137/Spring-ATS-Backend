package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.ApplicantInterview;

public interface ApplicantInterviewRepository extends JpaRepository<ApplicantInterview, Long> {

    List<ApplicantInterview> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    Optional<ApplicantInterview> findByIdAndApplicantId(Long id, Long applicantId);

    List<ApplicantInterview> findByScheduledDateBetweenAndReminder24hSentIsNullAndCompletedFalse(
        LocalDateTime from,
        LocalDateTime to
    );

    List<ApplicantInterview> findByScheduledDateBetweenAndReminder1hSentIsNullAndCompletedFalse(
        LocalDateTime from,
        LocalDateTime to
    );

    List<ApplicantInterview> findByApplicant_Facility_IdAndScheduledDateBetweenOrderByScheduledDate(
        Long facilityId,
        LocalDateTime from,
        LocalDateTime to
    );

    @Query("""
        SELECT i FROM ApplicantInterview i
        WHERE i.applicant.facility.id = :facilityId
          AND i.scheduledDate BETWEEN :from AND :to
          AND i.completed = false
          AND i.noShow = false
        ORDER BY i.scheduledDate
        """)
    List<ApplicantInterview> findUpcomingByFacilityId(
        @Param("facilityId") Long facilityId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT i FROM ApplicantInterview i
        WHERE i.scheduledDate BETWEEN :from AND :to
          AND i.completed = false
          AND i.noShow = false
        ORDER BY i.scheduledDate
        """)
    List<ApplicantInterview> findUpcomingAll(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    List<ApplicantInterview> findByScheduledDateBetweenOrderByScheduledDate(
        LocalDateTime from,
        LocalDateTime to
    );

    boolean existsByApplicant_Facility_IdAndScheduledDateBetweenAndCompletedFalseAndNoShowFalse(
        Long facilityId,
        LocalDateTime from,
        LocalDateTime to
    );

    List<ApplicantInterview> findByApplicant_Facility_IdAndScheduledDateBetweenAndCompletedFalseAndNoShowFalseOrderByScheduledDate(
        Long facilityId,
        LocalDateTime from,
        LocalDateTime to
    );
}
