package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmailRecipientRequestDto;
import com.example.demo.dto.EmailRecipientStatusPatchDto;
import com.example.demo.dto.EmailRecipientUpdateDto;
import com.example.demo.model.EmailRecipient;
import com.example.demo.model.User;
import com.example.demo.responses.FacilityAccessResponse;
import com.example.demo.responses.MessageResponse;
import com.example.demo.service.FacilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public ResponseEntity<List<FacilityAccessResponse>> getFacilities(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false, defaultValue = "false") Boolean includeInactive,
        @RequestParam(required = false) String search
    ) {
        List<FacilityAccessResponse> response = facilityService.getAccessibleFacilities(
            currentUser,
            includeInactive,
            search
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{facilityId}")
    public ResponseEntity<FacilityAccessResponse> getFacilityById(
        @PathVariable Long facilityId,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(facilityService.getAccessibleFacilityById(facilityId, currentUser));
    }

    
    @PostMapping("/{facilityId}/email-recipients")
    public ResponseEntity<EmailRecipient> addEmailRecipient(
        @PathVariable Long facilityId,
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody EmailRecipientRequestDto requestDto
    ) {
        EmailRecipient createdRecipient = facilityService.addEmailRecipient(facilityId, currentUser, requestDto);
        return ResponseEntity.ok(createdRecipient);
    }

    @GetMapping("/{facilityId}/email-recipients")
    public ResponseEntity<List<EmailRecipient>> getEmailRecipients(
        @PathVariable Long facilityId,
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) Boolean isActive
    ) {
        List<EmailRecipient> recipients = facilityService.getEmailRecipients(facilityId, currentUser, isActive);
        return ResponseEntity.ok(recipients);
    }

    @PatchMapping("/{facilityId}/email-recipients/{recipientId}")
    public ResponseEntity<EmailRecipient> updateEmailRecipient(
        @PathVariable Long facilityId,
        @PathVariable Long recipientId,
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody EmailRecipientUpdateDto requestDto
    ) {
        EmailRecipient updatedRecipient = facilityService.updateEmailRecipient(
            facilityId,
            recipientId,
            currentUser,
            requestDto
        );
        return ResponseEntity.ok(updatedRecipient);
    }

    @PatchMapping("/{facilityId}/email-recipients/{recipientId}/status")
    public ResponseEntity<EmailRecipient> updateEmailRecipientStatus(
        @PathVariable Long facilityId,
        @PathVariable Long recipientId,
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody EmailRecipientStatusPatchDto requestDto
    ) {
        EmailRecipient updatedRecipient = facilityService.updateEmailRecipientStatus(
            facilityId,
            recipientId,
            currentUser,
            requestDto.getIsActive()
        );
        return ResponseEntity.ok(updatedRecipient);
    }

    @DeleteMapping("/{facilityId}/email-recipients/{recipientId}")
    public ResponseEntity<MessageResponse> deleteEmailRecipient(
        @PathVariable Long facilityId,
        @PathVariable Long recipientId,
        @AuthenticationPrincipal User currentUser
    ) {
        facilityService.deleteEmailRecipient(facilityId, recipientId, currentUser);
        MessageResponse response = new MessageResponse("Email recipient removed successfully");
        return ResponseEntity.ok().body(response);
    }
}
