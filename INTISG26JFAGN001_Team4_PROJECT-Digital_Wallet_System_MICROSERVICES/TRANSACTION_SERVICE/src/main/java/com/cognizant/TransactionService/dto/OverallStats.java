package com.cognizant.TransactionService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OverallStats {
    private long totalTransactions;
    private BigDecimal netFlow;
}
