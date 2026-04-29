package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.EmailRecipient;

public interface EmailRecipientRepository extends JpaRepository<EmailRecipient, Long> {

    boolean existsByFacilityIdAndEmailIgnoreCase(Long facilityId, String email);

    boolean existsByFacilityId(Long facilityId);

    long countByFacilityId(Long facilityId);

    boolean existsByFacilityIdAndEmailIgnoreCaseAndIdNot(Long facilityId, String email, Long id);

    List<EmailRecipient> findByFacilityId(Long facilityId);

    List<EmailRecipient> findByFacilityIdAndIsActive(Long facilityId, Boolean isActive);

    Optional<EmailRecipient> findByIdAndFacilityId(Long id, Long facilityId);
}