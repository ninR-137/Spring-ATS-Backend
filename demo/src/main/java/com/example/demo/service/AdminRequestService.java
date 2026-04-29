package com.example.demo.service;

import com.example.demo.exception.DuplicateAdminRequestException;
import com.example.demo.exception.RequestAlreadyProcessedException;
import com.example.demo.exception.RequestNotFoundException;
import com.example.demo.model.AdminRequest;
import com.example.demo.model.AdminRequestStatus;
import com.example.demo.model.Role;
import com.example.demo.model.RoleName;
import com.example.demo.model.User;
import com.example.demo.repository.AdminRequestRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminRequestService {
    private final AdminRequestRepository adminRequestRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminRequestService(
            AdminRequestRepository adminRequestRepository,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.adminRequestRepository = adminRequestRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public AdminRequest createRequest(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (hasRole(user, RoleName.ROLE_ADMIN)) {
            throw new DuplicateAdminRequestException("User already has admin access");
        }

        if (adminRequestRepository.existsByUserAndStatus(user, AdminRequestStatus.PENDING)) {
            throw new DuplicateAdminRequestException("A pending admin request already exists for this user");
        }

        AdminRequest adminRequest = new AdminRequest();
        adminRequest.setUser(user);
        adminRequest.setStatus(AdminRequestStatus.PENDING);
        adminRequest.setCreatedAt(LocalDateTime.now());
        return adminRequestRepository.save(adminRequest);
    }

    @Transactional(readOnly = true)
    public List<AdminRequest> getMyRequests(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return adminRequestRepository.findAllByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<AdminRequest> getPendingRequests() {
        return adminRequestRepository.findAllByStatusOrderByCreatedAtAsc(AdminRequestStatus.PENDING);
    }

    @Transactional
    public AdminRequest reviewRequest(Long requestId, AdminRequestStatus status) {
        if (status == AdminRequestStatus.PENDING) {
            throw new IllegalArgumentException("Review status must be APPROVED or REJECTED");
        }

        AdminRequest adminRequest = adminRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Admin request not found"));

        if (adminRequest.getStatus() != AdminRequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException("Admin request has already been processed");
        }

        adminRequest.setStatus(status);
        adminRequest.setReviewedAt(LocalDateTime.now());

        if (status == AdminRequestStatus.APPROVED) {
            User user = userRepository.findById(adminRequest.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.getRoles().add(getOrCreateRole(RoleName.ROLE_ADMIN));
            userRepository.save(user);
        }

        return adminRequestRepository.save(adminRequest);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private Role getOrCreateRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
    }
}