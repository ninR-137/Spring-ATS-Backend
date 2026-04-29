package com.example.demo.responses;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReminderSentResponse {

    private String message;
    private List<String> recipients;
    private String reminderType;
    private LocalDateTime sentAt;
}
