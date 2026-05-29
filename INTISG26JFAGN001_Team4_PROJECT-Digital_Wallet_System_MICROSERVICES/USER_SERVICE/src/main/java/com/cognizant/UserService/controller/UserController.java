package com.cognizant.UserService.controller;

import com.cognizant.UserService.dto.UpdateUserDetailsDto;
import com.cognizant.UserService.dto.UserDetailsDto;
import com.cognizant.UserService.dto.UserLookupResponseDto;
import com.cognizant.UserService.entity.User;
import com.cognizant.UserService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/getProfile")
    public ResponseEntity<UserDetailsDto> getProfile(@AuthenticationPrincipal User user){
        return userService.getProfile(user);
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<UserDetailsDto> updateProfile(@AuthenticationPrincipal User user, @Valid @RequestBody UpdateUserDetailsDto updateUserDetailsDto){
        return userService.updateProfile(user, updateUserDetailsDto);
    }

    // Internal endpoint for service-to-service lookup (used by WALLET-SERVICE to resolve username -> userId)
    @GetMapping("/internal/by-username/{username}")
    public ResponseEntity<UserLookupResponseDto> getUserByUsername(@PathVariable String username){
        return userService.getUserByUsername(username);
    }

    // Internal endpoint for service-to-service lookup (used by WALLET-SERVICE to resolve userId -> username for error messages)
    @GetMapping("/internal/by-id/{userId}")
    public ResponseEntity<UserLookupResponseDto> getUserByUserId(@PathVariable Long userId){
        return userService.getUserByUserId(userId);
    }
}
