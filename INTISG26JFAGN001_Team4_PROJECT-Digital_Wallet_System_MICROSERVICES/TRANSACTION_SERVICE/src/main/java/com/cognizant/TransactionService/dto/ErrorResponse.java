package com.cognizant.TransactionService.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private Instant timeStamp;
    private int status;
    private String errorCode;
    private String message;
    private String path;
    private Map<String, String> errors; // field level validation errors
}
