package com.tamaspinter.instantpaymentapi.service;

import com.tamaspinter.instantpaymentapi.dto.AccountRequest;
import com.tamaspinter.instantpaymentapi.entity.Account;
import com.tamaspinter.instantpaymentapi.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountService {

    private static final String DEFAULT_ACCOUNT_NAME = "Default Account";

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(AccountRequest request) {
        Account newAccount = new Account(BigDecimal.ZERO);
        String accountName = request.accountName();
        String ownerName = request.ownerName();
        if (accountName == null) {
            newAccount.setAccountName(DEFAULT_ACCOUNT_NAME);
        } else {
            newAccount.setAccountName(request.accountName());
        }
        newAccount.setOwnerName(ownerName);
        return accountRepository.save(newAccount);
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }
}
