package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BatchEmailRecipientsDto;
import com.example.demo.dto.EmailRequestDto;
import com.example.demo.dto.FacilityRequestDto;
import com.example.demo.model.EmailRecipient;
import com.example.demo.model.Facility;
import com.example.demo.model.User;
import com.example.demo.responses.FacilityDeleteResponse;
import com.example.demo.responses.FacilityDetailResponse;
import com.example.demo.responses.FacilityRecoverResponse;
import com.example.demo.responses.FacilitySummaryResponse;
import com.example.demo.responses.FacilityUserRemovalResponse;
import com.example.demo.responses.FacilityUserResponse;
import com.example.demo.responses.MessageResponse;
import com.example.demo.service.AdminActionsService;
import com.example.demo.service.FacilityService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/admin")
public class AdminActionsController {

    private final AdminActionsService adminActionsService;
    private final UserService userService;
    private final FacilityService facilityService;

    public AdminActionsController(
        AdminActionsService adminActionsService,
        UserService userService,
        FacilityService facilityService
    ) {
        this.adminActionsService = adminActionsService;
        this.userService = userService;
        this.facilityService = facilityService;
    }
    

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/facilities/add")
    public ResponseEntity<Facility> createFacility(@Valid @RequestBody FacilityRequestDto facilityRequestDto) {
        Facility newFacility = new Facility();
        newFacility.setName(facilityRequestDto.getName());
        newFacility.setLocation(facilityRequestDto.getLocation());
        
        Facility createdFacility = adminActionsService.createFacility(newFacility);
        return ResponseEntity.ok(createdFacility);
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/facilities/{facilityId}/addUser")
    public ResponseEntity<?> addUserToFacility(
        @PathVariable Long facilityId,
        @Valid @RequestBody EmailRequestDto emailRequestDto
    ) {
        User user = userService.getUserByEmail(emailRequestDto.getEmail());
        adminActionsService.assignFacilityToUser(facilityId, user.getId());

        
        MessageResponse response = new MessageResponse("User added to facility successfully");
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/facilities/{facilityId}/email-recipients/batch")
    public ResponseEntity<List<EmailRecipient>> batchAddEmailRecipients(
        @PathVariable Long facilityId,
        @Valid @RequestBody BatchEmailRecipientsDto batchRequest
    ) {
        List<EmailRecipient> createdRecipients = facilityService.batchAddEmailRecipients(
            facilityId,
            batchRequest.getRecipients()
        );
        return ResponseEntity.ok(createdRecipients);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/facilities")
    public ResponseEntity<List<FacilitySummaryResponse>> getAllFacilities() {
        return ResponseEntity.ok(adminActionsService.getAllFacilities());
    }

    @PutMapping("/facilities/{facilityId}")
    public ResponseEntity<FacilityDetailResponse> updateFacility(
        @PathVariable Long facilityId,
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody FacilityRequestDto request
    ) {
        return ResponseEntity.ok(
            adminActionsService.updateFacility(facilityId, request.getName(), request.getLocation(), currentUser)
        );
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/facilities/{facilityId}")
    public ResponseEntity<FacilityDeleteResponse> deleteFacility(@PathVariable Long facilityId) {
        return ResponseEntity.ok(adminActionsService.deleteFacility(facilityId));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/facilities/{facilityId}/recover")
    public ResponseEntity<FacilityRecoverResponse> recoverFacility(@PathVariable Long facilityId) {
        return ResponseEntity.ok(adminActionsService.recoverFacility(facilityId));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/facilities/{facilityId}/users")
    public ResponseEntity<List<FacilityUserResponse>> getFacilityUsers(@PathVariable Long facilityId) {
        return ResponseEntity.ok(adminActionsService.getFacilityUsers(facilityId));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/facilities/{facilityId}/users/{userId}")
    public ResponseEntity<FacilityUserRemovalResponse> removeFacilityUser(
        @PathVariable Long facilityId,
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminActionsService.removeUserFromFacility(facilityId, userId));
    }
    
}
