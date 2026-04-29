package com.example.demo.responses;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilitySummaryResponse {

    private Long id;
    private String name;
    private String location;
    private LocalDateTime createdAt;
    private long userCount;
    private long applicantCount;
    private long emailRecipientCount;
}
