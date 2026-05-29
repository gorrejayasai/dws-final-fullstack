package com.cognizant.UserService.config;

import com.cognizant.UserService.entity.User;
import com.cognizant.UserService.enums.UserRole;
import com.cognizant.UserService.enums.UserStatus;
import com.cognizant.UserService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // runs after AdminSeeder (Order 1)
public class TestUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "admin123";

    private static final List<String[]> TEST_USERS = List.of(
            // username, email
            new String[]{"alice",   "alice@test.com"},
            new String[]{"bob",     "bob@test.com"},
            new String[]{"charlie", "charlie@test.com"},
            new String[]{"diana",   "diana@test.com"},
            new String[]{"evan",    "evan@test.com"},
            new String[]{"fiona",   "fiona@test.com"},
            new String[]{"george",  "george@test.com"},
            new String[]{"hannah",  "hannah@test.com"},
            new String[]{"ivan",    "ivan@test.com"},
            new String[]{"julia",   "julia@test.com"}
    );

    @Override
    public void run(String... args) throws Exception {
        String hashed = passwordEncoder.encode(PASSWORD);
        for (String[] u : TEST_USERS) {
            String username = u[0];
            String email    = u[1];
            if (userRepository.findByUsername(username).isEmpty()) {
                userRepository.save(
                        User.builder()
                                .username(username)
                                .email(email)
                                .password(hashed)
                                .role(UserRole.USER)
                                .status(UserStatus.ACTIVE)
                                .build()
                );
                log.info("Test user created: {}", username);
            } else {
                log.info("Test user already exists, skipping: {}", username);
            }
        }
    }
}
