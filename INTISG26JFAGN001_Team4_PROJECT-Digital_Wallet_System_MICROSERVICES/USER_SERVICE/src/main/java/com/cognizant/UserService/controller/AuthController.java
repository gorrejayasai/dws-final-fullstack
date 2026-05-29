package com.cognizant.UserService.controller;

import com.cognizant.UserService.dto.*;
import com.cognizant.UserService.entity.User;
import com.cognizant.UserService.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto signupRequestDto){
        return authService.signup(signupRequestDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto){
        return authService.login(loginRequestDto);
    }

    @GetMapping("/validate")
    public ResponseEntity<JwtValidationResponseDto> validate(@AuthenticationPrincipal User user){
        return authService.validate(user);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto){
        return authService.refreshToken(refreshTokenRequestDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDto> logout(@AuthenticationPrincipal User user){
        return authService.logout(user);
    }
}
