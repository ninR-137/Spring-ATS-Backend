package com.example.demo.exception;

public class RequestAlreadyProcessedException extends RuntimeException {
    public RequestAlreadyProcessedException(String message) {
        super(message);
    }
}