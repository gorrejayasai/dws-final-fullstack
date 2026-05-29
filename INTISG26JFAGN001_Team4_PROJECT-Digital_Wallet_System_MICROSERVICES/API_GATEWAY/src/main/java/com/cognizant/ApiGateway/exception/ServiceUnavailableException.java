package com.cognizant.ApiGateway.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message){
        super(message);
    }
}
