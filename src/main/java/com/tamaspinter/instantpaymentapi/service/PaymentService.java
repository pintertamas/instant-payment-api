package com.tamaspinter.instantpaymentapi.service;

import com.tamaspinter.instantpaymentapi.dto.PaymentRequest;
import com.tamaspinter.instantpaymentapi.entity.Account;
import com.tamaspinter.instantpaymentapi.entity.PaymentTransaction;
import com.tamaspinter.instantpaymentapi.repository.AccountRepository;
import com.tamaspinter.instantpaymentapi.repository.PaymentTransactionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private static final String TOPIC_TRANSACTION_NOTIFICATION = "transaction_notifications";

    public final AccountRepository accountRepository;
    public final PaymentTransactionRepository paymentTransactionRepository;
    public final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentService(AccountRepository accountRepository, PaymentTransactionRepository paymentTransactionRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public PaymentTransaction processPayment(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new RuntimeException("Cannot transfer to the same account");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        Account fromAccount = accountRepository.findById(request.fromAccountId())
                .orElseThrow(() -> new RuntimeException("From Account not found"));
        Account toAccount = accountRepository.findById(request.toAccountId())
                .orElseThrow(() -> new RuntimeException("To Account not found"));

        BigDecimal amount = request.amount();
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        PaymentTransaction transaction = new PaymentTransaction(
                fromAccount.getId(), toAccount.getId(), amount
        );
        transaction = paymentTransactionRepository.save(transaction);

        String message = String.format(
                "Payment of %s from account %d to account %d succeeded.",
                amount, fromAccount.getId(), toAccount.getId()
        );
        kafkaTemplate.send(TOPIC_TRANSACTION_NOTIFICATION, message);

        return transaction;
    }
}
