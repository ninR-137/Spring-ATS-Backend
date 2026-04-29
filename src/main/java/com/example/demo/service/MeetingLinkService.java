package com.example.demo.service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.model.ApplicantInterview;
import com.example.demo.model.ApplicantOrientation;

@Service
public class MeetingLinkService {

    @Value("${meeting.provider:jitsi}")
    private String meetingProvider;

    public String generateMeetingLink(ApplicantInterview interview) {
        return switch (meetingProvider.toLowerCase()) {
            case "jitsi" -> "https://meet.jit.si/" + jitsiRoomName();
            case "zoom" -> "https://zoom.us/j/" + randomNumericId(10);
            case "teams" -> "https://teams.microsoft.com/l/meetup-join/" + UUID.randomUUID();
            default -> "https://meet.jit.si/" + jitsiRoomName();
        };
    }

    public String generateOrientationLink(ApplicantOrientation orientation) {
        return switch (meetingProvider.toLowerCase()) {
            case "jitsi" -> "https://meet.jit.si/" + jitsiRoomName();
            case "zoom" -> "https://zoom.us/j/" + randomNumericId(10);
            case "teams" -> "https://teams.microsoft.com/l/meetup-join/" + UUID.randomUUID();
            default -> "https://meet.jit.si/" + jitsiRoomName();
        };
    }

    public String randomPassword() {
        return randomAlpha(6).toUpperCase();
    }

    /** Generates a Jitsi-style room name: two random words joined by a hyphen, e.g. "PurpleTiger-48291" */
    private String jitsiRoomName() {
        String[] adjectives = {"Swift","Brave","Calm","Bold","Keen","Wise","Bright","Clear","Sharp","Cool"};
        String[] nouns = {"Tiger","Falcon","River","Summit","Cedar","Stone","Bridge","Cloud","Harbor","Forest"};
        int adj = ThreadLocalRandom.current().nextInt(adjectives.length);
        int noun = ThreadLocalRandom.current().nextInt(nouns.length);
        int num = ThreadLocalRandom.current().nextInt(10000, 99999);
        return adjectives[adj] + nouns[noun] + "-" + num;
    }

    /** Generates a random numeric string of the given length (no leading zeros). */
    private String randomNumericId(int length) {
        StringBuilder sb = new StringBuilder(length);
        sb.append((char) ('1' + ThreadLocalRandom.current().nextInt(9)));
        for (int i = 1; i < length; i++) {
            sb.append((char) ('0' + ThreadLocalRandom.current().nextInt(10)));
        }
        return sb.toString();
    }

    /** Generates a random lowercase alphabetic string of the given length. */
    private String randomAlpha(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
        }
        return sb.toString();
    }
}
