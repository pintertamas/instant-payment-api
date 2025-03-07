package com.tamaspinter.instantpaymentapi.dto;

import java.math.BigDecimal;

public record DepositRequest(
        Long accountId,
        BigDecimal amount) {
}
