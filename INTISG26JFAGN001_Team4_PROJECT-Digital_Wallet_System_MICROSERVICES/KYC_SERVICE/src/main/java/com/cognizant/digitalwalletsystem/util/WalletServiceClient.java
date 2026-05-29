package com.cognizant.digitalwalletsystem.util;

import com.cognizant.digitalwalletsystem.dto.CreateWalletRequest;
import com.cognizant.digitalwalletsystem.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("WALLET-SERVICE")
public interface WalletServiceClient {

    @PostMapping("/wallets")
    WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest req);
}
