package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.ApplicantOrientation;

public interface ApplicantOrientationRepository extends JpaRepository<ApplicantOrientation, Long> {

    Optional<ApplicantOrientation> findByApplicantId(Long applicantId);

    Optional<ApplicantOrientation> findByIdAndApplicantId(Long id, Long applicantId);

    List<ApplicantOrientation> findByApplicant_Facility_IdAndScheduledDateBetweenOrderByScheduledDate(
        Long facilityId,
        LocalDateTime from,
        LocalDateTime to
    );

    @Query("""
        SELECT o FROM ApplicantOrientation o
        WHERE o.applicant.facility.id = :facilityId
          AND o.scheduledDate BETWEEN :from AND :to
          AND o.completed = false
          AND o.noShow = false
        ORDER BY o.scheduledDate
        """)
    List<ApplicantOrientation> findUpcomingByFacilityId(
        @Param("facilityId") Long facilityId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT o FROM ApplicantOrientation o
        WHERE o.scheduledDate BETWEEN :from AND :to
          AND o.completed = false
          AND o.noShow = false
        ORDER BY o.scheduledDate
        """)
    List<ApplicantOrientation> findUpcomingAll(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
