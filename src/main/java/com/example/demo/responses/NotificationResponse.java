package com.example.demo.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private String message;
    private boolean emailSent;
    private boolean calendarInviteSent;
}
