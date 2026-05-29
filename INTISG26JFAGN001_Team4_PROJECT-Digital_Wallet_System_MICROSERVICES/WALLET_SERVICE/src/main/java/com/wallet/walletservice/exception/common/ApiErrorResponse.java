package com.wallet.walletservice.exception.common;

public record ApiErrorResponse(
        String timestamp, int status, String error,
        String code, String message, String path, String traceId
) {}