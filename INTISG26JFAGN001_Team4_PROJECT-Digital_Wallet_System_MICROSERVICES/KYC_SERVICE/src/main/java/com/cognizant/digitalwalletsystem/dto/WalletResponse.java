package com.cognizant.digitalwalletsystem.dto;

import com.cognizant.digitalwalletsystem.entity.enums.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

//This DTO handles wallet creation after KYC approval by admin
public class WalletResponse {

    private Long id;
    private Long userId;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;
    private WalletStatus status;
    private Instant createdAt;
}
