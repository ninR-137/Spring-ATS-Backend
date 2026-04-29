package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRequestDto {
    
    @NotBlank(message = "Email is required")
    private String email;

}
