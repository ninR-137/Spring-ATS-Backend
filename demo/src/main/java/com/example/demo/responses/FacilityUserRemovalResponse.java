package com.example.demo.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacilityUserRemovalResponse {

    private final String message;
    private final Long facilityId;
    private final Long userId;
    private final String userEmail;
}
