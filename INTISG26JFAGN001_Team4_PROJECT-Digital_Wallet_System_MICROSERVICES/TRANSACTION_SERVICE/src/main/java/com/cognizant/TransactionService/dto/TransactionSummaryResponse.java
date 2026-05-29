package com.cognizant.TransactionService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSummaryResponse {
    private Long walletId;
    private String currency;
    private BigDecimal currentBalance;
    private AmountCount topup;
    private AmountCount withdraw;
    private AmountCount transfersSent;
    private AmountCount transfersReceived;
    private OverallStats overall;
}
