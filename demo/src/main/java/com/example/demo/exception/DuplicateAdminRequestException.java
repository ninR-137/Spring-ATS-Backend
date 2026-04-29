package com.example.demo.exception;

public class DuplicateAdminRequestException extends RuntimeException {
    public DuplicateAdminRequestException(String message) {
        super(message);
    }
}