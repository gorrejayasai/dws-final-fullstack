package com.wallet.walletservice.config;

import com.wallet.walletservice.exception.ExternalServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (String methodKey, Response response) -> {
            // 4xx: let Feign throw its typed exception (e.g. FeignException.NotFound)
            // so callers can distinguish business cases like "user not found"
            // from a genuine service outage.
            if (response.status() >= 400 && response.status() < 500) {
                return defaultDecoder.decode(methodKey, response);
            }
            String serviceName = methodKey.split("#")[0]
                    .replace("ServiceClient", " Service");
            return new ExternalServiceException(
                    serviceName, "returned HTTP " + response.status());
        };
    }
}