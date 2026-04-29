package com.example.demo.responses;

import lombok.Getter;

@Getter
public class MessageResponse {
    
    private String message;


    public MessageResponse(String message) {
        this.message = message;
    }
}
