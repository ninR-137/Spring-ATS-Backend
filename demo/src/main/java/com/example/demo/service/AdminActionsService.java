package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.EmailRecipient;
import com.example.demo.model.ApplicantStatus;

import com.example.demo.model.Facility;
import com.example.demo.model.RoleName;
import com.example.demo.model.User;
import com.example.demo.repository.ApplicantRepository;
import com.example.demo.repository.EmailRecipientRepository;
import com.example.demo.repository.FacilityRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.responses.FacilityDeleteResponse;
import com.example.demo.responses.FacilityDetailResponse;
import com.example.demo.responses.FacilityRecoverResponse;
import com.example.demo.responses.FacilitySummaryResponse;
import com.example.demo.responses.FacilityUserRemovalResponse;
import com.example.demo.responses.FacilityUserResponse;


@Service
public class AdminActionsService {
    
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final ApplicantRepository applicantRepository;
    private final EmailRecipientRepository emailRecipientRepository;
    private final ApplicantService applicantService;


    public AdminActionsService(
        FacilityRepository facilityRepository,
        UserRepository userRepository,
        ApplicantRepository applicantRepository,
        EmailRecipientRepository emailRecipientRepository,
        ApplicantService applicantService
    ) {
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.applicantRepository = applicantRepository;
        this.emailRecipientRepository = emailRecipientRepository;
        this.applicantService = applicantService;
    }


    @Transactional
    public Facility createFacility(Facility facility) {
        return facilityRepository.save(facility);
    }

    @Transactional
    public void assignFacilityToUser(Long facilityId, Long userId) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if user already has access to the facility
        if(facility.getUsers().contains(user)) {
            throw new IllegalArgumentException("User already has access to this facility");
        }


        user.getFacilities().add(facility);   // owning side
        facility.getUsers().add(user);        // inverse side sync (optional)
        userRepository.save(user);            // persist join table row
    }

    @Transactional(readOnly = true)
    public List<FacilitySummaryResponse> getAllFacilities() {
        return facilityRepository.findAll().stream()
            .sorted(Comparator.comparing(Facility::getId))
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public FacilityDetailResponse getFacilityById(Long facilityId, User currentUser) {
        ensureCanAccessFacility(facilityId, currentUser);
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));
        return toDetail(facility);
    }

    @Transactional
    public FacilityDetailResponse updateFacility(Long facilityId, String name, String location, User currentUser) {
        ensureCanAccessFacility(facilityId, currentUser);
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));

        facility.setName(name.trim());
        facility.setLocation(location.trim());
        Facility saved = facilityRepository.save(facility);
        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<FacilityUserResponse> getFacilityUsers(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));

        return facility.getUsers().stream()
            .sorted(Comparator.comparing(User::getId))
            .map(this::toFacilityUser)
            .toList();
    }

    @Transactional
    public FacilityUserRemovalResponse removeUserFromFacility(Long facilityId, Long userId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.getFacilities().contains(facility)) {
            throw new IllegalArgumentException("User is not assigned to this facility");
        }

        if (isAdmin(user) && countAdminUsers(facility) <= 1) {
            throw new IllegalArgumentException("Cannot remove last admin user from facility");
        }

        user.getFacilities().remove(facility);
        facility.getUsers().remove(user);
        userRepository.save(user);

        return new FacilityUserRemovalResponse(
            "User removed from facility successfully",
            facilityId,
            userId,
            user.getEmail()
        );
    }

    @Transactional
    public FacilityDeleteResponse deleteFacility(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));

        int archivedApplicants = applicantService.archiveApplicantsByFacilityId(facilityId);

        List<User> assignedUsers = userRepository.findByFacilities_Id(facilityId);
        for (User user : assignedUsers) {
            user.getFacilities().remove(facility);
        }
        userRepository.saveAll(assignedUsers);

        List<EmailRecipient> recipients = emailRecipientRepository.findByFacilityId(facilityId);
        recipients.forEach(recipient -> recipient.setIsActive(false));
        emailRecipientRepository.saveAll(recipients);

        String facilityName = facility.getName();
        facility.setActive(false);
        facility.setDeletedAt(LocalDateTime.now());
        facilityRepository.save(facility);
        return new FacilityDeleteResponse(
            "Facility archived successfully. Archived applicants: " + archivedApplicants,
            facilityId,
            facilityName
        );
    }

    @Transactional
    public FacilityRecoverResponse recoverFacility(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));

        if (facility.isActive()) {
            return new FacilityRecoverResponse(
                "Facility is already active",
                facility.getId(),
                facility.getName(),
                0
            );
        }

        facility.setActive(true);
        facility.setDeletedAt(null);
        facilityRepository.save(facility);

        int recoveredApplicants = applicantService.recoverApplicantsByFacilityId(facilityId);

        List<EmailRecipient> recipients = emailRecipientRepository.findByFacilityId(facilityId);
        recipients.forEach(recipient -> recipient.setIsActive(true));
        emailRecipientRepository.saveAll(recipients);

        return new FacilityRecoverResponse(
            "Facility recovered successfully",
            facility.getId(),
            facility.getName(),
            recoveredApplicants
        );
    }

    private FacilitySummaryResponse toSummary(Facility facility) {
        FacilitySummaryResponse response = new FacilitySummaryResponse();
        response.setId(facility.getId());
        response.setName(facility.getName());
        response.setLocation(facility.getLocation());
        response.setCreatedAt(facility.getCreatedAt());
        response.setUserCount(userRepository.countByFacilities_Id(facility.getId()));
        response.setApplicantCount(applicantRepository.countByFacilityIdAndIsArchivedFalse(facility.getId()));
        response.setEmailRecipientCount(emailRecipientRepository.countByFacilityId(facility.getId()));
        return response;
    }

    private FacilityDetailResponse toDetail(Facility facility) {
        FacilityDetailResponse response = new FacilityDetailResponse();
        response.setId(facility.getId());
        response.setName(facility.getName());
        response.setLocation(facility.getLocation());
        response.setCreatedAt(facility.getCreatedAt());
        response.setUpdatedAt(facility.getUpdatedAt());
        response.setUsers(getFacilityUsers(facility.getId()));

        long total = applicantRepository.countByFacilityIdAndIsArchivedFalse(facility.getId());
        long hired = applicantRepository.countByFacilityIdAndStatusAndIsArchivedFalse(facility.getId(), ApplicantStatus.HIRED);
        long inactive = applicantRepository.countByFacilityIdAndStatusAndIsArchivedFalse(facility.getId(), ApplicantStatus.NOT_QUALIFIED)
            + applicantRepository.countByFacilityIdAndStatusAndIsArchivedFalse(facility.getId(), ApplicantStatus.NOT_ELIGIBLE_FOR_HIRE)
            + applicantRepository.countByFacilityIdAndStatusAndIsArchivedFalse(facility.getId(), ApplicantStatus.NOT_INTERESTED)
            + applicantRepository.countByFacilityIdAndStatusAndIsArchivedFalse(facility.getId(), ApplicantStatus.INTERVIEW_NO_SHOW)
            + applicantRepository.countByFacilityIdAndStatusAndIsArchivedFalse(facility.getId(), ApplicantStatus.ORIENTATION_NO_SHOW)
            + applicantRepository.countByFacilityIdAndStatusAndIsArchivedFalse(facility.getId(), ApplicantStatus.ORIENTATION_COMPLETED);

        FacilityDetailResponse.FacilityStatsResponse stats = new FacilityDetailResponse.FacilityStatsResponse();
        stats.setTotalApplicants(total);
        stats.setHiredApplicants(hired);
        stats.setActiveApplicants(Math.max(0, total - inactive - hired));
        stats.setEmailRecipients(emailRecipientRepository.countByFacilityId(facility.getId()));
        response.setStats(stats);
        return response;
    }

    private FacilityUserResponse toFacilityUser(User user) {
        FacilityUserResponse response = new FacilityUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRoles().stream()
            .map(role -> role.getName().name().replace("ROLE_", ""))
            .sorted()
            .findFirst()
            .orElse("USER"));
        response.setAddedAt(null);
        return response;
    }

    private int countAdminUsers(Facility facility) {
        Set<User> users = facility.getUsers();
        if (users == null || users.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (User u : users) {
            if (isAdmin(u)) {
                count++;
            }
        }
        return count;
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null
            && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    private void ensureCanAccessFacility(Long facilityId, User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (isAdmin(currentUser)) {
            return;
        }

        boolean hasFacilityAccess = facilityRepository.existsByIdAndUsers_Id(facilityId, currentUser.getId());
        if (!hasFacilityAccess) {
            throw new AccessDeniedException("User does not have access to this facility");
        }
    }
}
