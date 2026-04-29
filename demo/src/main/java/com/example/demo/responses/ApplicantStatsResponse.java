package com.example.demo.responses;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicantStatsResponse {

    private long totalApplicants;
    private Map<String, Long> byStatus;
    private Map<String, Long> byRole;
    private List<FacilityCountResponse> byFacility;
    private double conversionRate;
    private double averageTimeToHire;
}
