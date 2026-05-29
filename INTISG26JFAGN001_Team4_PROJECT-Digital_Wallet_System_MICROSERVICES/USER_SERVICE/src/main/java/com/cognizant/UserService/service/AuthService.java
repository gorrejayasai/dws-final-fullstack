package com.cognizant.UserService.service;

import com.cognizant.UserService.dto.*;
import com.cognizant.UserService.entity.RefreshToken;
import com.cognizant.UserService.entity.User;
import com.cognizant.UserService.enums.UserRole;
import com.cognizant.UserService.repository.RefreshTokenRepository;
import com.cognizant.UserService.repository.UserRepository;
import com.cognizant.UserService.util.AuthUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthUtil authUtil;
    private final PasswordEncoder passwordEncoder;

    public ResponseEntity<SignupResponseDto> signup(SignupRequestDto signupRequestDto){

        // Validate input
        if(signupRequestDto.getUsername() == null || signupRequestDto.getUsername().trim().isEmpty()){
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if(signupRequestDto.getPassword() == null || signupRequestDto.getPassword().isEmpty()){
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if(signupRequestDto.getEmail() ==  null || signupRequestDto.getEmail().trim().isEmpty()){
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if(!signupRequestDto.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")){
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check for duplicate username
        Optional<User> optUser = userRepository.findByUsername(signupRequestDto.getUsername());
        Optional<User> optEmail = userRepository.findByEmail(signupRequestDto.getEmail());

        if(optUser.isPresent()){
            throw new IllegalArgumentException("Username already exists");
        }
        if(optEmail.isPresent()){
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userRepository.save(User
                .builder()
                .username(signupRequestDto.getUsername())
                .email(signupRequestDto.getEmail())
                .password(passwordEncoder.encode(signupRequestDto.getPassword()))
                .role(UserRole.USER)
                .build());

        return ResponseEntity.ok(SignupResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build());
    }

    public ResponseEntity<LoginResponseDto> login(LoginRequestDto loginRequestDto){

        // here the authenticationManager checks if the user is in the db or not
        // the authenticationManager is implemented by the ProviderManager which has a list of AuthenticationProvider and among them one is DaoAuthenticationProvider
        // the DaoAuthenticationProvider uses UserDetailsService and PasswordEncoder internally
        // we have implemented UserDetailsService in our project by CustomUserDetailsService
        // we have also created a bean of PasswordEncoder
        // once the authenticationProvider calls the authenticate() with the raw unauthenticated UsernamePasswordToken(), then the daoAuth provider checks if the user is in the db or not by using our UserDetailService's loadByUsername method and if the user exists then it matches the password with the encoded password using our encoder bean and then if the password matches then it sends the Authentication object which has principal(UserDetails)
        // if the password doesn't match then a BadCredentialsException is thrown.
        // if user is not found using username then UsernameNotFoundException is thrown.
        Authentication authentication = authenticationManager
                .authenticate( new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword())
                );

        User user = (User) authentication.getPrincipal();

        String accessToken = authUtil.generateAccessToken(user);
        String refreshTokenString = authUtil.generateRefreshToken(user);

        // Store refresh token in separate table
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return ResponseEntity.ok(LoginResponseDto.builder()
                .jwt(accessToken)
                .refreshToken(refreshTokenString)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build());
    }

//    public ResponseEntity<JwtValidationResponseDto> validate(String headerToken){
//        try {
//            // Validate header token
//            if(headerToken == null || headerToken.trim().isEmpty()){
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(JwtValidationResponseDto.builder().isValid(false).build());
//            }
//
//            if(!headerToken.startsWith("Bearer ")){
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(JwtValidationResponseDto.builder().isValid(false).build());
//            }
//
//            String token = headerToken.substring(7);
//
//            String username = authUtil.getUsernameFromToken(token);
//
//            User user = userRepository.findByUsername(username)
//                    .orElse(null);
//
//            if(user == null){
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(JwtValidationResponseDto.builder().isValid(false).build());
//            }
//
//            return ResponseEntity.ok(JwtValidationResponseDto.builder()
//                    .isValid(true)
//                    .userId(user.getId())
//                    .username(user.getUsername())
//                    .email(user.getEmail())
//                    .role(user.getRole())
//                    .status(user.getStatus())
//                    .createdAt(user.getCreatedAt())
//                    .updatedAt(user.getUpdatedAt())
//                    .build());
//
//        } catch (Exception e) {
//            // Covers expired/malformed/invalid JWT and any other unexpected errors
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(JwtValidationResponseDto.builder().isValid(false).build());
//        }
//    }

    public ResponseEntity<JwtValidationResponseDto> validate(User user) {
        // user is injected by Spring Security from the SecurityContext
        // if we're here, the token was valid — the filter guaranteed it
        return ResponseEntity.ok(JwtValidationResponseDto.builder()
                .isValid(true)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build());
    }

    public ResponseEntity<RefreshTokenResponseDto> refreshToken(
            RefreshTokenRequestDto refreshTokenRequestDto) {

        // Validate input
        if(refreshTokenRequestDto.getRefreshToken() == null || refreshTokenRequestDto.getRefreshToken().trim().isEmpty()){
            throw new IllegalArgumentException("Refresh token cannot be empty");
        }

        String refreshTokenString = refreshTokenRequestDto.getRefreshToken();

        // Find refresh token in database
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Validate token is not revoked
        if(Boolean.TRUE.equals(storedToken.getIsRevoked())){
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        // Validate token is not expired
        if(LocalDateTime.now().isAfter(storedToken.getExpiresAt())){
            throw new IllegalArgumentException("Refresh token has expired");
        }

        User user = storedToken.getUser();
        
        // Validate user status is active
        if(user == null || !user.getStatus().toString().equals("ACTIVE")){
            throw new IllegalArgumentException("User account is not active");
        }
        
        String newAccessToken = authUtil.generateAccessToken(user);
        String newRefreshTokenString = authUtil.generateRefreshToken(user);

        // Revoke old token
        storedToken.setIsRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Create new refresh token
        LocalDateTime newRefreshTokenExpiresAt = LocalDateTime.now().plusDays(7);
        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenString)
                .user(user)
                .expiresAt(newRefreshTokenExpiresAt)
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return ResponseEntity.ok(RefreshTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenString)
                .accessTokenExpiresAt(LocalDateTime.now().plusMinutes(10))
                .refreshTokenExpiresAt(newRefreshTokenExpiresAt)
                .build());
    }

    @Transactional
    public ResponseEntity<LogoutResponseDto> logout(User user){
        // Validate user
        if(user == null){
            throw new IllegalArgumentException("User not authenticated");
        }
        
        // Revoke all refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        return ResponseEntity.ok(LogoutResponseDto.builder()
                .message("Logged out successfully")
                .success(true)
                .logoutAt(LocalDateTime.now())
                .build());
    }
}
