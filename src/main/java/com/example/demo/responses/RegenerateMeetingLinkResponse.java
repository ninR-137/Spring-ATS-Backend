package com.example.demo.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegenerateMeetingLinkResponse {

    private Long interviewId;
    private String oldMeetingLink;
    private String newMeetingLink;
    private String newMeetingPassword;
    private boolean calendarEventUpdated;
    private boolean notificationsSent;
    private String message;
}
