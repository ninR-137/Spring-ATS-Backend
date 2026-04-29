package com.example.demo.dto;

import com.example.demo.model.NotificationType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendNotificationRequest {

    @NotNull(message = "notificationType is required")
    private NotificationType notificationType;
}
