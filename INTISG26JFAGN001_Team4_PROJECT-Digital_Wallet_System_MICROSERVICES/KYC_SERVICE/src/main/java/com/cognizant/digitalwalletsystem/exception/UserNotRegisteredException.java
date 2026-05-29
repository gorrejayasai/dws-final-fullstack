package com.cognizant.digitalwalletsystem.exception;

public class UserNotRegisteredException extends RuntimeException {

    public UserNotRegisteredException(Long userId) {
        super("User with ID " + userId + " is not registered in the system.");
    }
}
