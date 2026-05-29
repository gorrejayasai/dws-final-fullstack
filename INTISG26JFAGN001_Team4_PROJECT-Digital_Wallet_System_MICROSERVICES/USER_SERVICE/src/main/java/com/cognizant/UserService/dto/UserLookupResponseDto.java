package com.cognizant.UserService.dto;

import com.cognizant.UserService.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLookupResponseDto {
    private Long userId;
    private String username;
    private UserStatus status;
}