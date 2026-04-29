package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.ApplicantInterview;
import com.example.demo.repository.ApplicantInterviewRepository;

@Component
public class ReminderScheduler {

    private final EmailNotificationService emailNotificationService;
    private final ApplicantInterviewRepository interviewRepository;

    public ReminderScheduler(
        EmailNotificationService emailNotificationService,
        ApplicantInterviewRepository interviewRepository
    ) {
        this.emailNotificationService = emailNotificationService;
        this.interviewRepository = interviewRepository;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime target24h = now.plusHours(24);
        List<ApplicantInterview> interviewsFor24h = interviewRepository
            .findByScheduledDateBetweenAndReminder24hSentIsNullAndCompletedFalse(
                target24h.minusMinutes(1),
                target24h.plusMinutes(1)
            );

        for (ApplicantInterview interview : interviewsFor24h) {
            if (emailNotificationService.sendInterviewReminder(interview, "24h")) {
                interview.setReminder24hSent(now);
                interviewRepository.save(interview);
            }
        }

        LocalDateTime target1h = now.plusHours(1);
        List<ApplicantInterview> interviewsFor1h = interviewRepository
            .findByScheduledDateBetweenAndReminder1hSentIsNullAndCompletedFalse(
                target1h.minusMinutes(1),
                target1h.plusMinutes(1)
            );

        for (ApplicantInterview interview : interviewsFor1h) {
            if (emailNotificationService.sendInterviewReminder(interview, "1h")) {
                interview.setReminder1hSent(now);
                interviewRepository.save(interview);
            }
        }
    }
}
