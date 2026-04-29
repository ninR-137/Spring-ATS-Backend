package com.example.demo.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacilityDeleteResponse {

    private final String message;
    private final Long facilityId;
    private final String facilityName;
}
