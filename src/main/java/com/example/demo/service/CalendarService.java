package com.example.demo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.model.Applicant;
import com.example.demo.model.ApplicantInterview;
import com.example.demo.model.ApplicantOrientation;
import com.example.demo.model.EmailRecipient;

@Service
public class CalendarService {

    public String addInterviewToCalendar(Applicant applicant, ApplicantInterview interview, List<EmailRecipient> recipients) {
        return buildCalendarId("interview", applicant.getId(), interview.getScheduledDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public String addOrientationToCalendar(Applicant applicant, ApplicantOrientation orientation, List<EmailRecipient> recipients) {
        return buildCalendarId("orientation", applicant.getId(), orientation.getScheduledDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private String buildCalendarId(String prefix, Long applicantId, String slot) {
        return prefix + "-" + applicantId + "-" + slot.hashCode() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
