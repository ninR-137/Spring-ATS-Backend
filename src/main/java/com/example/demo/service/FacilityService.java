package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.EmailRecipientRequestDto;
import com.example.demo.dto.EmailRecipientUpdateDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.EmailRecipient;
import com.example.demo.model.Facility;
import com.example.demo.model.RoleName;
import com.example.demo.model.User;
import com.example.demo.repository.ApplicantRepository;
import com.example.demo.repository.EmailRecipientRepository;
import com.example.demo.repository.FacilityRepository;
import com.example.demo.responses.FacilityAccessResponse;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final EmailRecipientRepository emailRecipientRepository;
    private final ApplicantRepository applicantRepository;


    public FacilityService(
        FacilityRepository facilityRepository,
        EmailRecipientRepository emailRecipientRepository,
        ApplicantRepository applicantRepository
    ) {
        this.facilityRepository = facilityRepository;
        this.emailRecipientRepository = emailRecipientRepository;
        this.applicantRepository = applicantRepository;
    }

    @Transactional(readOnly = true)
    public List<FacilityAccessResponse> getAccessibleFacilities(User currentUser, Boolean includeInactive, String search) {
        boolean admin = isAdmin(currentUser);
        boolean showInactive = admin && Boolean.TRUE.equals(includeInactive);
        String normalizedSearch = normalizeSearch(search);

        List<Facility> facilities = facilityRepository.findAccessibleFacilities(
            currentUser.getId(),
            admin,
            showInactive
        );

        if (normalizedSearch != null) {
            facilities = facilities.stream()
                .filter(facility -> matchesSearch(facility, normalizedSearch))
                .toList();
        }

        return facilities.stream()
            .map(facility -> toAccessResponse(facility, admin))
            .toList();
    }

    @Transactional
    public EmailRecipient addEmailRecipient(Long facilityId, User currentUser, EmailRecipientRequestDto requestDto) {
        ensureUserCanAccessFacility(facilityId, currentUser);
        String normalizedEmail = normalizeEmailOrThrow(requestDto.getEmail());

        if (emailRecipientRepository.existsByFacilityIdAndEmailIgnoreCase(facilityId, normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists for this facility");
        }

        EmailRecipient recipient = new EmailRecipient();
        recipient.setFacilityId(facilityId);
        recipient.setEmail(normalizedEmail);
        recipient.setName(normalizeName(requestDto.getName()));
        recipient.setIsActive(requestDto.getIsActive() == null ? true : requestDto.getIsActive());
        return emailRecipientRepository.save(recipient);
    }

    @Transactional(readOnly = true)
    public List<EmailRecipient> getEmailRecipients(Long facilityId, User currentUser, Boolean isActive) {
        ensureUserCanAccessFacility(facilityId, currentUser);
        if (isActive != null) {
            return emailRecipientRepository.findByFacilityIdAndIsActive(facilityId, isActive);
        }
        return emailRecipientRepository.findByFacilityId(facilityId);
    }

    @Transactional
    public EmailRecipient updateEmailRecipient(
        Long facilityId,
        Long recipientId,
        User currentUser,
        EmailRecipientUpdateDto requestDto
    ) {
        ensureUserCanAccessFacility(facilityId, currentUser);
        EmailRecipient recipient = emailRecipientRepository.findByIdAndFacilityId(recipientId, facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Email recipient not found"));

        if (requestDto.getEmail() != null) {
            String normalizedEmail = normalizeEmailOrThrow(requestDto.getEmail());
            if (emailRecipientRepository.existsByFacilityIdAndEmailIgnoreCaseAndIdNot(facilityId, normalizedEmail, recipientId)) {
                throw new IllegalArgumentException("Email already exists for this facility");
            }
            recipient.setEmail(normalizedEmail);
        }

        if (requestDto.getName() != null) {
            recipient.setName(normalizeName(requestDto.getName()));
        }

        if (requestDto.getIsActive() != null) {
            recipient.setIsActive(requestDto.getIsActive());
        }

        return emailRecipientRepository.save(recipient);
    }

    @Transactional
    public EmailRecipient updateEmailRecipientStatus(
        Long facilityId,
        Long recipientId,
        User currentUser,
        Boolean isActive
    ) {
        ensureUserCanAccessFacility(facilityId, currentUser);
        EmailRecipient recipient = emailRecipientRepository.findByIdAndFacilityId(recipientId, facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Email recipient not found"));

        recipient.setIsActive(isActive);
        return emailRecipientRepository.save(recipient);
    }

    @Transactional
    public void deleteEmailRecipient(Long facilityId, Long recipientId, User currentUser) {
        ensureUserCanAccessFacility(facilityId, currentUser);
        EmailRecipient recipient = emailRecipientRepository.findByIdAndFacilityId(recipientId, facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Email recipient not found"));
        emailRecipientRepository.delete(recipient);
    }

    @Transactional
    public List<EmailRecipient> batchAddEmailRecipients(Long facilityId, List<EmailRecipientRequestDto> recipients) {
        ensureFacilityExists(facilityId);

        Set<String> requestEmails = new HashSet<>();
        List<EmailRecipient> entitiesToSave = new ArrayList<>();

        for (EmailRecipientRequestDto requestDto : recipients) {
            String normalizedEmail = normalizeEmailOrThrow(requestDto.getEmail());

            if (!requestEmails.add(normalizedEmail)) {
                throw new IllegalArgumentException("Duplicate email found in request: " + normalizedEmail);
            }

            if (emailRecipientRepository.existsByFacilityIdAndEmailIgnoreCase(facilityId, normalizedEmail)) {
                throw new IllegalArgumentException("Email already exists for this facility: " + normalizedEmail);
            }

            EmailRecipient recipient = new EmailRecipient();
            recipient.setFacilityId(facilityId);
            recipient.setEmail(normalizedEmail);
            recipient.setName(normalizeName(requestDto.getName()));
            recipient.setIsActive(requestDto.getIsActive() == null ? true : requestDto.getIsActive());
            entitiesToSave.add(recipient);
        }

        return emailRecipientRepository.saveAll(entitiesToSave);
    }


    @Transactional
    public Facility getFacilityById(Long facilityId, User currentUser) {
        ensureUserCanAccessFacility(facilityId, currentUser);
        return facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));
    }

    @Transactional(readOnly = true)
    public FacilityAccessResponse getAccessibleFacilityById(Long facilityId, User currentUser) {
        Facility facility = getFacilityById(facilityId, currentUser);
        return toAccessResponse(facility, isAdmin(currentUser));
    }

    private void ensureUserCanAccessFacility(Long facilityId, User currentUser) {
        ensureFacilityExists(facilityId);
        if (isAdmin(currentUser)) {
            return;
        }

        Long userId = currentUser.getId();
        boolean hasFacilityAccess = userId != null && facilityRepository.existsByIdAndUsers_Id(facilityId, userId);
        if (!hasFacilityAccess) {
            throw new AccessDeniedException("User does not have access to this facility");
        }
    }

    private void ensureFacilityExists(Long facilityId) {
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null
            && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.trim().isEmpty()) {
            return null;
        }
        return search.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(Facility facility, String search) {
        String name = facility.getName() == null ? "" : facility.getName().toLowerCase(Locale.ROOT);
        String location = facility.getLocation() == null ? "" : facility.getLocation().toLowerCase(Locale.ROOT);
        return name.contains(search) || location.contains(search);
    }

    private FacilityAccessResponse toAccessResponse(Facility facility, boolean adminView) {
        FacilityAccessResponse response = new FacilityAccessResponse();
        response.setId(facility.getId());
        response.setName(facility.getName());
        response.setLocation(facility.getLocation());
        response.setActive(facility.isActive());

        if (adminView) {
            response.setCreatedAt(facility.getCreatedAt());
            response.setDeletedAt(facility.getDeletedAt());
            response.setUserCount((long) facility.getUsers().size());
            response.setApplicantCount(applicantRepository.countByFacilityIdAndIsArchivedFalse(facility.getId()));
            response.setEmailRecipientCount(emailRecipientRepository.countByFacilityId(facility.getId()));
        } else {
            response.setAssignedAt(facility.getCreatedAt());
        }

        return response;
    }

    private String normalizeEmailOrThrow(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
