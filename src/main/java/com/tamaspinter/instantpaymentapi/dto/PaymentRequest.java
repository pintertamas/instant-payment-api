package com.tamaspinter.instantpaymentapi.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount) {
}
