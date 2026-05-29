package com.cognizant.ApiGateway.dto;

import com.cognizant.ApiGateway.enums.UserRole;
import com.cognizant.ApiGateway.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtValidationResponseDto {

    @JsonProperty("isValid")
    private Boolean isValid;
    private Long userId;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
