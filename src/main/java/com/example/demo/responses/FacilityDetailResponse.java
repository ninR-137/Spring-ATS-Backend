package com.example.demo.responses;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityDetailResponse {

    private Long id;
    private String name;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FacilityUserResponse> users;
    private FacilityStatsResponse stats;

    @Getter
    @Setter
    public static class FacilityStatsResponse {
        private long totalApplicants;
        private long activeApplicants;
        private long hiredApplicants;
        private long emailRecipients;
    }
}
