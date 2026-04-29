package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendReminderNowRequest {

    @NotBlank(message = "reminderType is required (24h or 1h)")
    private String reminderType;
}
