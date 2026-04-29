package com.example.demo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchEmailRecipientsDto {

    @NotEmpty(message = "Recipients list cannot be empty")
    private List<@Valid EmailRecipientRequestDto> recipients;
}
