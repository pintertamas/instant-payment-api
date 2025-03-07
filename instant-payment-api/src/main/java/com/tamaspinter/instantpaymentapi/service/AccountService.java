package com.tamaspinter.instantpaymentapi.service;

import com.tamaspinter.instantpaymentapi.dto.AccountRequest;
import com.tamaspinter.instantpaymentapi.dto.DepositRequest;
import com.tamaspinter.instantpaymentapi.entity.Account;
import com.tamaspinter.instantpaymentapi.repository.AccountRepository;
import jakarta.ws.rs.NotFoundException;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;

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

    public Account getAccountById(Long id) throws AccountNotFoundException {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isPresent()) {
            return account.get();
        } else {
            throw new AccountNotFoundException("Account not found");
        }
    }

    public Account deposit(DepositRequest request) throws AccountNotFoundException {
        Account account = getAccountById(request.accountId());
        if (request.amount().intValue() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        BigDecimal amount = request.amount();
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }
}
