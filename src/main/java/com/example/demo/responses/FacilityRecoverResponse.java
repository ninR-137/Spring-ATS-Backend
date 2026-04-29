package com.example.demo.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacilityRecoverResponse {

    private final String message;
    private final Long facilityId;
    private final String facilityName;
    private final int recoveredApplicants;
}
