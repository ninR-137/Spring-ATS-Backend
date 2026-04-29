package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.demo.model.Applicant;
import com.example.demo.model.ApplicantInterview;
import com.example.demo.model.ApplicantOrientation;
import com.example.demo.model.EmailRecipient;
import com.example.demo.model.EmailTemplate;
import com.example.demo.model.InterviewType;
import com.example.demo.repository.EmailRecipientRepository;

import jakarta.mail.MessagingException;

@Service
public class EmailNotificationService {

    private static final DateTimeFormatter EMAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter EMAIL_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private final EmailService emailService;
    private final EmailRecipientRepository emailRecipientRepository;
    private final TemplateEngine templateEngine;

    public EmailNotificationService(
        EmailService emailService,
        EmailRecipientRepository emailRecipientRepository,
        TemplateEngine templateEngine
    ) {
        this.emailService = emailService;
        this.emailRecipientRepository = emailRecipientRepository;
        this.templateEngine = templateEngine;
    }

    public boolean sendScheduledInterviewEmail(Applicant applicant, ApplicantInterview interview) {
        Map<String, String> recipients = buildScheduledNotificationRecipients(applicant);
        if (recipients.isEmpty()) {
            return false;
        }

        String teamSubject = "Interview Scheduled: " + applicant.getName() + " - " + applicant.getRole();
        String applicantSubject = "Interview Scheduled for " + applicant.getRole() + " position";
        String applicantEmail = normalizeEmail(applicant.getEmail());

        for (Map.Entry<String, String> recipient : recipients.entrySet()) {
            String targetEmail = recipient.getKey();
            String targetName = recipient.getValue();
            String subject = targetEmail.equalsIgnoreCase(applicantEmail) ? applicantSubject : teamSubject;
            sendQuietly(
                targetEmail,
                subject,
                renderTemplate("interview-scheduled", buildInterviewScheduledModel(targetName, applicant, interview))
            );
        }
        return true;
    }

    public boolean sendInterviewCompletedEmail(Applicant applicant, ApplicantInterview interview) {
        List<EmailRecipient> recipients = getActiveRecipients(applicant);
        if (CollectionUtils.isEmpty(recipients)) {
            return false;
        }

        String status = interview.isNoShow() ? "No Show" : "Completed";
        String subject = "Interview " + status + ": " + applicant.getName();

        for (EmailRecipient recipient : recipients) {
            sendQuietly(
                recipient.getEmail(),
                subject,
                renderTemplate("interview-completed", buildInterviewCompletedModel(recipient.getName(), applicant, interview, status))
            );
        }
        return true;
    }

    public boolean sendOrientationScheduledEmail(Applicant applicant, ApplicantOrientation orientation) {
        Map<String, String> recipients = buildScheduledNotificationRecipients(applicant);
        if (recipients.isEmpty()) {
            return false;
        }

        String subject = "Orientation Scheduled: " + applicant.getName();
        for (Map.Entry<String, String> recipient : recipients.entrySet()) {
            sendQuietly(
                recipient.getKey(),
                subject,
                renderTemplate("orientation-scheduled", buildOrientationScheduledModel(recipient.getValue(), applicant, orientation))
            );
        }
        return true;
    }

    public boolean sendHiredEmail(Applicant applicant) {
        List<EmailRecipient> recipients = getActiveRecipients(applicant);
        if (CollectionUtils.isEmpty(recipients)) {
            return false;
        }

        String subject = "Candidate Hired: " + applicant.getName() + " - " + applicant.getRole();
        for (EmailRecipient recipient : recipients) {
            sendQuietly(
                recipient.getEmail(),
                subject,
                renderTemplate("hired", buildHiredModel(recipient.getName(), applicant, false))
            );
        }

        sendQuietly(
            applicant.getEmail(),
            "You are hired",
            renderTemplate("hired", buildHiredModel(applicant.getName(), applicant, true))
        );
        return true;
    }

    public boolean sendStatusUpdate(Applicant applicant, String status, String notes) {
        List<EmailRecipient> recipients = getActiveRecipients(applicant);
        if (CollectionUtils.isEmpty(recipients)) {
            return false;
        }

        String subject = "Applicant status updated: " + applicant.getName();
        String body = "<p>Status changed to <b>" + status + "</b> for " + applicant.getName() + "</p>"
            + "<p>Notes: " + nullSafe(notes) + "</p>";
        for (EmailRecipient recipient : recipients) {
            sendQuietly(recipient.getEmail(), subject, body);
        }
        return true;
    }

    public boolean sendInterviewReminder(ApplicantInterview interview, String reminderWindow) {
        Applicant applicant = interview.getApplicant();
        List<EmailRecipient> recipients = getActiveRecipients(applicant);
        if (CollectionUtils.isEmpty(recipients)) {
            return false;
        }

        String subject = "Interview reminder (" + reminderWindow + "): " + applicant.getName();
        Map<String, Object> model = buildInterviewScheduledModel("Team", applicant, interview);
        model.put("emailTitle", "Interview Reminder");
        model.put("introText", "This is a " + reminderWindow + " reminder for an upcoming interview.");
        String body = renderTemplate("interview-scheduled", model);

        for (EmailRecipient recipient : recipients) {
            sendQuietly(recipient.getEmail(), subject, body);
        }
        return true;
    }

    public Map<String, Object> templateMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        for (EmailTemplate template : EmailTemplate.values()) {
            metadata.put(template.name().toLowerCase(Locale.ROOT), template.getTemplatePath());
        }
        return metadata;
    }

    private List<EmailRecipient> getActiveRecipients(Applicant applicant) {
        return emailRecipientRepository.findByFacilityIdAndIsActive(applicant.getFacility().getId(), true);
    }

    private Map<String, String> buildScheduledNotificationRecipients(Applicant applicant) {
        Map<String, String> recipients = new LinkedHashMap<>();

        for (EmailRecipient recipient : getActiveRecipients(applicant)) {
            String email = normalizeEmail(recipient.getEmail());
            if (StringUtils.hasText(email)) {
                recipients.put(email, StringUtils.hasText(recipient.getName()) ? recipient.getName().trim() : "Team");
            }
        }

        String applicantEmail = normalizeEmail(applicant.getEmail());
        if (StringUtils.hasText(applicantEmail)) {
            recipients.put(applicantEmail, StringUtils.hasText(applicant.getName()) ? applicant.getName().trim() : "Applicant");
        }

        return recipients;
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private void sendQuietly(String to, String subject, String htmlBody) {
        try {
            emailService.sendVerificationEmail(to, subject, htmlBody);
        } catch (MessagingException ignored) {
            // Keep workflow running even if one recipient mail fails.
        }
    }

    private Map<String, Object> buildInterviewScheduledModel(String recipientName, Applicant applicant, ApplicantInterview interview) {
        Map<String, Object> model = buildBaseModel(recipientName, applicant);
        boolean onSite = interview.getInterviewType() == InterviewType.IN_PERSON;

        model.put("emailTitle", "Interview Scheduled");
        model.put("introText", "An interview has been scheduled. Please review the details below.");
        model.put("sessionTypeLabel", formatInterviewType(interview.getInterviewType()));
        model.put("scheduledDateLabel", formatDate(interview.getScheduledDate()));
        model.put("scheduledTimeLabel", formatTimeRange(interview.getScheduledDate(), interview.getScheduledEndDate()));
        model.put("isOnSite", onSite);
        model.put("facilityLocation", nullSafe(applicant.getFacility().getLocation()));
        model.put("meetingLink", nullSafe(interview.getMeetingLink()));
        model.put("meetingId", nullSafe(interview.getMeetingId()));
        model.put("meetingPassword", nullSafe(interview.getMeetingPassword()));
        model.put("notes", nullSafe(interview.getNotes()));
        return model;
    }

    private Map<String, Object> buildInterviewCompletedModel(
        String recipientName,
        Applicant applicant,
        ApplicantInterview interview,
        String status
    ) {
        Map<String, Object> model = buildBaseModel(recipientName, applicant);
        model.put("emailTitle", "Interview " + status);
        model.put("introText", "The interview result has been recorded.");
        model.put("status", status);
        model.put("scheduledDateLabel", formatDate(interview.getScheduledDate()));
        model.put("scheduledTimeLabel", formatTimeRange(interview.getScheduledDate(), interview.getScheduledEndDate()));
        model.put("notes", nullSafe(interview.getNotes()));
        return model;
    }

    private Map<String, Object> buildOrientationScheduledModel(
        String recipientName,
        Applicant applicant,
        ApplicantOrientation orientation
    ) {
        Map<String, Object> model = buildBaseModel(recipientName, applicant);
        boolean onSite = !StringUtils.hasText(orientation.getMeetingLink());

        model.put("emailTitle", "Orientation Scheduled");
        model.put("introText", "Orientation details are ready. Please review the schedule below.");
        model.put("scheduledDateLabel", formatDate(orientation.getScheduledDate()));
        model.put("scheduledTimeLabel", formatTimeRange(orientation.getScheduledDate(), orientation.getScheduledEndDate()));
        model.put("isOnSite", onSite);
        model.put("facilityLocation", nullSafe(applicant.getFacility().getLocation()));
        model.put("meetingLink", nullSafe(orientation.getMeetingLink()));
        model.put(
            "documentsRequired",
            orientation.getDocumentsRequired() == null || orientation.getDocumentsRequired().isEmpty()
                ? "-"
                : orientation.getDocumentsRequired().stream().filter(StringUtils::hasText).collect(Collectors.joining(", "))
        );
        model.put("notes", nullSafe(orientation.getNotes()));
        return model;
    }

    private Map<String, Object> buildHiredModel(String recipientName, Applicant applicant, boolean applicantCopy) {
        Map<String, Object> model = buildBaseModel(recipientName, applicant);
        model.put("emailTitle", applicantCopy ? "Congratulations" : "Candidate Hired");
        model.put(
            "introText",
            applicantCopy
                ? "Congratulations, you have been selected for the role."
                : "The applicant below has been marked as HIRED."
        );
        model.put("isApplicantCopy", applicantCopy);
        return model;
    }

    private Map<String, Object> buildBaseModel(String recipientName, Applicant applicant) {
        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", StringUtils.hasText(recipientName) ? recipientName.trim() : "Team");
        model.put("applicantName", applicant.getName());
        model.put("role", applicant.getRole());
        model.put("facilityName", applicant.getFacility() == null ? "-" : nullSafe(applicant.getFacility().getName()));
        return model;
    }

    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context(Locale.US);
        context.setVariables(variables);
        return templateEngine.process("email-templates/" + templateName, context);
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime.format(EMAIL_DATE_FORMAT);
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        String startText = start.format(EMAIL_TIME_FORMAT);
        if (end == null) {
            return startText + " (local time)";
        }
        return startText + " - " + end.format(EMAIL_TIME_FORMAT) + " (local time)";
    }

    private String formatInterviewType(InterviewType interviewType) {
        if (interviewType == null) {
            return "Interview";
        }

        return switch (interviewType) {
            case PHONE -> "Phone";
            case VIDEO -> "Video";
            case IN_PERSON -> "On-site";
        };
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }
}
