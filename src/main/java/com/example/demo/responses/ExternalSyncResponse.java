package com.example.demo.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalSyncResponse {

    private String message;
    private int syncedEvents;
    private String calendarId;
    private String syncToken;
}
