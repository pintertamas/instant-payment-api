package com.tamaspinter.instantpaymentapi.controller;

import com.tamaspinter.instantpaymentapi.dto.AccountRequest;
import com.tamaspinter.instantpaymentapi.entity.Account;
import com.tamaspinter.instantpaymentapi.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody AccountRequest request) {
        Account account = accountService.createAccount(request);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable Long accountId) {
        try {
            Account account = accountService.getAccountById(accountId);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
