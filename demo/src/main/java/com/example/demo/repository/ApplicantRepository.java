package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Applicant;
import com.example.demo.model.ApplicantStatus;

public interface ApplicantRepository extends JpaRepository<Applicant, Long>, JpaSpecificationExecutor<Applicant> {

    boolean existsByFacilityIdAndEmailIgnoreCaseAndIsArchivedFalse(Long facilityId, String email);

    boolean existsByFacilityId(Long facilityId);

    boolean existsByFacilityIdAndIsArchivedFalse(Long facilityId);

    long countByFacilityId(Long facilityId);

    long countByFacilityIdAndIsArchivedFalse(Long facilityId);

    long countByFacilityIdAndStatus(Long facilityId, ApplicantStatus status);

    long countByFacilityIdAndStatusAndIsArchivedFalse(Long facilityId, ApplicantStatus status);

    boolean existsByFacilityIdAndEmailIgnoreCaseAndIdNotAndIsArchivedFalse(Long facilityId, String email, Long id);

    Optional<Applicant> findByIdAndIsArchivedFalse(Long id);

    Optional<Applicant> findByIdAndIsArchivedTrue(Long id);

    List<Applicant> findByFacilityIdAndIsArchivedFalse(Long facilityId);

    List<Applicant> findByFacilityIdAndIsArchivedTrue(Long facilityId);

    @Query("""
        select a from Applicant a
        where (:facilityId is null or a.facility.id = :facilityId)
          and a.isArchived = false
          and (:status is null or a.status = :status)
          and (
              lower(a.name) like lower(concat('%', :query, '%'))
              or lower(a.email) like lower(concat('%', :query, '%'))
              or lower(coalesce(a.phoneNumber, '')) like lower(concat('%', :query, '%'))
              or lower(a.role) like lower(concat('%', :query, '%'))
          )
        order by a.addedDate desc
        """)
    List<Applicant> searchApplicants(
        @Param("query") String query,
        @Param("facilityId") Long facilityId,
        @Param("status") ApplicantStatus status,
        Pageable pageable
    );
}
