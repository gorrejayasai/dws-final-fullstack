package com.cognizant.ApiGateway.config;

import com.cognizant.ApiGateway.exception.InvalidTokenException;
import com.cognizant.ApiGateway.exception.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String s, Response response) {
        return switch (response.status()) {
            case 401 -> new InvalidTokenException("Token invalid or expired");
            case 403 -> new InvalidTokenException("Token forbidden");
            default  -> new ServiceUnavailableException("Unexpected error from user-service: " + response.status());
        };
    }
}
