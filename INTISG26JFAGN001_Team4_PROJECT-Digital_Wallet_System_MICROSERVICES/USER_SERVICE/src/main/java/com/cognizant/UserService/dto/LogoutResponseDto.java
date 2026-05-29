package com.cognizant.UserService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogoutResponseDto {

    private String message;
    private boolean success;
    private LocalDateTime logoutAt;
}

