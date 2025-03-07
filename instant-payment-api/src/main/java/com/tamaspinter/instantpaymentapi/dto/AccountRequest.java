package com.tamaspinter.instantpaymentapi.dto;

public record AccountRequest(
        String accountName,
        String ownerName) {
}
