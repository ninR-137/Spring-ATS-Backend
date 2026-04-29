package com.example.demo.model;

public enum EmailTemplate {
    SCHEDULED("templates/email-templates/interview-scheduled.html"),
    INTERVIEW_COMPLETED("templates/email-templates/interview-completed.html"),
    ORIENTATION_SCHEDULED("templates/email-templates/orientation-scheduled.html"),
    HIRED("templates/email-templates/hired.html");

    private final String templatePath;

    EmailTemplate(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getTemplatePath() {
        return templatePath;
    }
}
