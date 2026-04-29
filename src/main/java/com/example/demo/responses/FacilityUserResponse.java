package com.example.demo.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityUserResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String addedAt;
}
