package com.cognizant.UserService.service;

import com.cognizant.UserService.dto.UpdateUserDetailsDto;
import com.cognizant.UserService.dto.UserDetailsDto;
import com.cognizant.UserService.dto.UserLookupResponseDto;
import com.cognizant.UserService.entity.User;
import com.cognizant.UserService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public ResponseEntity<UserDetailsDto> getProfile(User user){
        // Validate user
        if(user == null){
            throw new IllegalArgumentException("User not authenticated");
        }
        
        return ResponseEntity.ok(UserDetailsDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build());
    }

    public ResponseEntity<UserDetailsDto> updateProfile(User user, UpdateUserDetailsDto updateUserDetailsDto){
        // Validate user
        if(user == null){
            throw new IllegalArgumentException("User not authenticated");
        }
        
        // Validate at least one field is provided for update
        if((updateUserDetailsDto.getUsername() == null || updateUserDetailsDto.getUsername().trim().isEmpty()) &&
           (updateUserDetailsDto.getEmail() == null || updateUserDetailsDto.getEmail().trim().isEmpty())){
            throw new IllegalArgumentException("At least one field (username or email) must be provided for update");
        }
        
        // Update username if provided and unique
        if(updateUserDetailsDto.getUsername() != null && !updateUserDetailsDto.getUsername().trim().isEmpty()){
            if(!updateUserDetailsDto.getUsername().equals(user.getUsername())){
                // Check if new username is already taken
                if(userRepository.findByUsername(updateUserDetailsDto.getUsername()).isPresent()){
                    throw new IllegalArgumentException("Username already taken");
                }
            }
            user.setUsername(updateUserDetailsDto.getUsername());
        }

        // Update email if provided
        if(updateUserDetailsDto.getEmail() != null && !updateUserDetailsDto.getEmail().trim().isEmpty()){
            user.setEmail(updateUserDetailsDto.getEmail());
        }

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(UserDetailsDto.builder()
                .userId(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole())
                .status(updatedUser.getStatus())
                .createdAt(updatedUser.getCreatedAt())
                .updatedAt(updatedUser.getUpdatedAt())
                .build());
    }

    public ResponseEntity<UserLookupResponseDto> getUserByUsername(String username){
        if(username == null || username.trim().isEmpty()){
            throw new IllegalArgumentException("Username cannot be blank");
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found"));
        return ResponseEntity.ok(UserLookupResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .build());
    }

    public ResponseEntity<UserLookupResponseDto> getUserByUserId(Long userId){
        if(userId == null){
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User with id " + userId + " not found"));
        return ResponseEntity.ok(UserLookupResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .build());
    }
}
