package com.cognizant.digitalwalletsystem.config;

import com.cognizant.digitalwalletsystem.exception.UserNotRegisteredException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Global OpenFeign configuration//

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new UserServiceErrorDecoder();
    }

    //converts specific HTTP error codes from the User Service into meaningful domain exceptions.

    static class UserServiceErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            if (response.status() == 404) {

                // Extracts userId from the request URL (last segment of the path)
                String url = response.request().url();
                String[] parts = url.split("/");
                try {
                    Long userId = Long.parseLong(parts[parts.length - 2]); // …/{userId}/exists
                    return new UserNotRegisteredException(userId);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                    return new UserNotRegisteredException(null);
                }
            }
            return defaultDecoder.decode(methodKey, response);
        }
    }
}