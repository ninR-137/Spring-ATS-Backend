package com.example.demo.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Applicant;
import com.example.demo.model.RoleName;
import com.example.demo.model.User;
import com.example.demo.repository.FacilityRepository;

@Service
public class ApplicantPermissionService {

    private final FacilityRepository facilityRepository;

    public ApplicantPermissionService(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    public boolean isAdmin(User user) {
        return user != null
            && user.getRoles() != null
            && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    public void ensureAdmin(User user) {
        if (!isAdmin(user)) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    public boolean canAccessApplicant(User user, Applicant applicant) {
        if (isAdmin(user)) {
            return true;
        }
        if (user == null || user.getId() == null || applicant == null || applicant.getFacility() == null) {
            return false;
        }
        return facilityRepository.existsByIdAndUsers_Id(applicant.getFacility().getId(), user.getId());
    }

    public void validateApplicantAccess(User user, Applicant applicant) {
        if (!canAccessApplicant(user, applicant)) {
            throw new AccessDeniedException("No access to this applicant");
        }
    }

    public void validateFacilityAccess(User user, Long facilityId) {
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found");
        }
        if (isAdmin(user)) {
            return;
        }
        if (user == null || user.getId() == null
            || !facilityRepository.existsByIdAndUsers_Id(facilityId, user.getId())) {
            throw new AccessDeniedException("No access to this facility");
        }
    }
}
