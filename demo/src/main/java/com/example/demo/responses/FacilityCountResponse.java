package com.example.demo.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacilityCountResponse {
    private Long facilityId;
    private String facilityName;
    private Long count;
}
