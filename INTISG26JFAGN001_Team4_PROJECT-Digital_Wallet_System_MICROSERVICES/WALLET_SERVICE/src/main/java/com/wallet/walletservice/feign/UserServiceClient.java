
package com.wallet.walletservice.feign;

import com.wallet.walletservice.dto.response.UserLookupResponse;
import com.wallet.walletservice.feign.fallback.UserServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "USER-SERVICE",
        fallback = UserServiceFallback.class
)
public interface UserServiceClient {

    @GetMapping("/user/internal/by-username/{username}")
    UserLookupResponse getUserByUsername(@PathVariable("username") String username);

    @GetMapping("/user/internal/by-id/{userId}")
    UserLookupResponse getUserByUserId(@PathVariable("userId") Long userId);
}