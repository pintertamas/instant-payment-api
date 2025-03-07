package com.tamaspinter.instantpaymentapi;

import com.tamaspinter.instantpaymentapi.dto.PaymentRequest;
import com.tamaspinter.instantpaymentapi.entity.Account;
import com.tamaspinter.instantpaymentapi.entity.PaymentTransaction;
import com.tamaspinter.instantpaymentapi.repository.AccountRepository;
import com.tamaspinter.instantpaymentapi.repository.PaymentTransactionRepository;
import com.tamaspinter.instantpaymentapi.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(
                accountRepository,
                paymentTransactionRepository,
                kafkaTemplate
        );
    }

    /**
     * Intended success scenario
     */
    @Test
    void testProcessPaymentSuccess() {
        Account fromAccount = new Account(new BigDecimal("100.00"));
        fromAccount.setId(1L);
        Account toAccount = new Account(new BigDecimal("50.00"));
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        PaymentRequest request = new PaymentRequest(1L, 2L, new BigDecimal("25.00"));

        PaymentTransaction result = paymentService.processPayment(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("75.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("75.00"), toAccount.getBalance());
        verify(kafkaTemplate, times(1)).send(anyString(), anyString());
    }

    /**
     * Insufficient funds
     * When the fromAccount doesn't have enough balance, an exception should be thrown.
     */
    @Test
    void testProcessPaymentInsufficientFunds() {
        Account fromAccount = new Account(new BigDecimal("10.00"));
        fromAccount.setId(1L);
        Account toAccount = new Account(new BigDecimal("50.00"));
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        PaymentRequest request = new PaymentRequest(1L, 2L, new BigDecimal("25.00"));

        Exception ex = assertThrows(InvalidRequestException.class, () ->
                paymentService.processPayment(request)
        );
        assertEquals("Insufficient balance", ex.getMessage());

        // Verify no transaction was saved and no Kafka message was sent
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    /**
     * From Account not found
     */
    @Test
    void testProcessPaymentFromAccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        when(accountRepository.findById(2L)).thenReturn(Optional.of(new Account()));

        PaymentRequest request = new PaymentRequest(1L, 2L, new BigDecimal("25.00"));

        Exception ex = assertThrows(EntityNotFoundException.class, () ->
                paymentService.processPayment(request)
        );
        assertEquals("From Account not found", ex.getMessage());

        // Verify nothing else happened
        verify(accountRepository, never()).save(any(Account.class));
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    /**
     * To Account not found
     */
    @Test
    void testProcessPaymentToAccountNotFound() {
        Account fromAccount = new Account(new BigDecimal("100.00"));
        fromAccount.setId(1L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(1L, 2L, new BigDecimal("25.00"));

        Exception ex = assertThrows(EntityNotFoundException.class, () ->
                paymentService.processPayment(request)
        );
        assertEquals("To Account not found", ex.getMessage());

        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    /**
     * Zero or negative amount transfer
     */
    @Test
    void testProcessPaymentZeroAmount() {
        Account fromAccount = new Account(new BigDecimal("100.00"));
        fromAccount.setId(1L);
        Account toAccount = new Account(new BigDecimal("50.00"));
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        PaymentRequest zeroAmountRequest = new PaymentRequest(1L, 2L, BigDecimal.ZERO);
        PaymentRequest negativeAmountRequest = new PaymentRequest(1L, 2L, BigDecimal.valueOf(-100.00));

        Exception zeroAmountException = assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(zeroAmountRequest)
        );

        Exception negativeAmountException = assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(negativeAmountRequest)
        );

        // Balances unchanged
        assertEquals(new BigDecimal("100.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("50.00"), toAccount.getBalance());

        assertEquals("Amount must be positive", zeroAmountException.getMessage());
        assertEquals("Amount must be positive", negativeAmountException.getMessage());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    /**
     * Large amount (BigDecimal allows for arbitrary precision so overflow is not a concern)
     */
    @Test
    void testProcessPaymentLargeAmount() {
        Account fromAccount = new Account(new BigDecimal("9999999999999999999999999999999.99"));
        fromAccount.setId(1L);
        Account toAccount = new Account(new BigDecimal("50.00"));
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        PaymentRequest request = new PaymentRequest(1L, 2L, new BigDecimal("5000000000000000000000000000000.00"));

        PaymentTransaction result = paymentService.processPayment(request);

        assertEquals(new BigDecimal("4999999999999999999999999999999.99"), fromAccount.getBalance());
        assertEquals(new BigDecimal("5000000000000000000000000000050.00"), toAccount.getBalance());

        assertNotNull(result);
        assertEquals(new BigDecimal("5000000000000000000000000000000.00"), result.getAmount());
        verify(kafkaTemplate, times(1)).send(anyString(), anyString());
    }

    /**
     * Same account for from and to
     */
    @Test
    void testProcessPaymentSameAccount() {
        Account sameAccount = new Account(new BigDecimal("100.00"));
        sameAccount.setId(1L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sameAccount));

        PaymentRequest request = new PaymentRequest(1L, 1L, new BigDecimal("10.00"));

        Exception ex = assertThrows(InvalidRequestException.class, () ->
                paymentService.processPayment(request)
        );
        assertEquals("Cannot transfer to the same account", ex.getMessage());

        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    /**
     * Validate Kafka sends the correct message
     */
    @Test
    void testProcessPaymentKafkaMessageContent() {
        Account fromAccount = new Account(new BigDecimal("200.00"));
        fromAccount.setId(1L);
        Account toAccount = new Account(new BigDecimal("300.00"));
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        PaymentRequest request = new PaymentRequest(1L, 2L, new BigDecimal("50.00"));

        paymentService.processPayment(request);

        verify(kafkaTemplate, times(1)).send(anyString(), argThat((String msg) ->
                msg.contains("Payment of 50.00") &&
                        msg.contains("from account 1") &&
                        msg.contains("to account 2 succeeded.")
        ));
    }

    /**
     * Sending everything to another account.
     * It should succeed, but leave fromAccount with 0.00 balance.
     */
    @Test
    void testProcessPaymentExactBalance() {
        Account fromAccount = new Account(new BigDecimal("25.00"));
        fromAccount.setId(1L);
        Account toAccount = new Account(new BigDecimal("100.00"));
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        PaymentRequest request = new PaymentRequest(1L, 2L, new BigDecimal("25.00"));

        PaymentTransaction result = paymentService.processPayment(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("125.00"), toAccount.getBalance());
        verify(kafkaTemplate, times(1)).send(anyString(), anyString());
    }

    @Test
    void testProcessPaymentWithNullRequest() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPayment(null)
        );
        assertEquals("Payment request cannot be null", ex.getMessage());
    }

    /**
     * Rollback on unexpected error
     * This test simulates an unexpected error during the processPayment method.
     *
     * NOTE: Unfortunately using mocks, it's not possible to simulate a real unexpected error,
     * so I tested it manually by creating a breakpoint in the processPayment method on the first database save() call,
     * then stopped the database service, and continued the execution.
     * I did get an internal server error as expected, and the accounts' balances remained unchanged.
     */
    @Test
    void testProcessPaymentRollbackOnUnexpectedError_fromAccount() {
        final BigDecimal starterBalance = new BigDecimal("100.00");
        Account fromAccount = new Account(starterBalance);
        fromAccount.setId(1L);
        Account toAccount = new Account(starterBalance);
        toAccount.setId(2L);

        when(accountRepository.findById(1L))
                .thenThrow(new DataAccessResourceFailureException("Simulated DB error on saving toAccount"))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L))
                .thenReturn(Optional.of(toAccount));

        assertThrows(RuntimeException.class, () -> paymentService.processPayment(
                new PaymentRequest(fromAccount.getId(), toAccount.getId(), BigDecimal.valueOf(25))
        ));

        Account fromAccountAfter = accountRepository.findById(fromAccount.getId()).orElseThrow();
        Account toAccountAfter = accountRepository.findById(toAccount.getId()).orElseThrow();

        assertEquals(starterBalance.multiply(BigDecimal.valueOf(2)), fromAccountAfter.getBalance().add(toAccountAfter.getBalance()));
        assertEquals(starterBalance, fromAccountAfter.getBalance());
        assertEquals(starterBalance, toAccountAfter.getBalance());

        verify(paymentTransactionRepository, org.mockito.Mockito.never()).save(any(PaymentTransaction.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void testProcessPaymentRollbackOnUnexpectedError_toAccount() {
        final BigDecimal starterBalance = new BigDecimal("100.00");
        Account fromAccount = new Account(starterBalance);
        fromAccount.setId(1L);
        Account toAccount = new Account(starterBalance);
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L))
                .thenThrow(new RuntimeException("Simulated DB error on saving toAccount"))
                .thenReturn(Optional.of(toAccount));

        assertThrows(RuntimeException.class, () -> paymentService.processPayment(
                new PaymentRequest(fromAccount.getId(), toAccount.getId(), BigDecimal.valueOf(25))
        ));

        Account fromAccountAfter = accountRepository.findById(fromAccount.getId()).orElseThrow();
        Account toAccountAfter = accountRepository.findById(toAccount.getId()).orElseThrow();

        assertEquals(starterBalance.multiply(BigDecimal.valueOf(2)), fromAccountAfter.getBalance().add(toAccountAfter.getBalance()));
        assertEquals(starterBalance, fromAccountAfter.getBalance());
        assertEquals(starterBalance, toAccountAfter.getBalance());

        verify(paymentTransactionRepository, org.mockito.Mockito.never()).save(any(PaymentTransaction.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void testDoubleSpendingConcurrently() throws Exception {
        Account fromAccount = new Account(new BigDecimal("100.00"));
        fromAccount.setId(1L);
        Account toAccount = new Account(new BigDecimal("0.00"));
        toAccount.setId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        PaymentRequest request1 = new PaymentRequest(1L, 2L, new BigDecimal("9.00"));
        PaymentRequest request2 = new PaymentRequest(1L, 2L, new BigDecimal("51.00"));
        PaymentRequest request3 = new PaymentRequest(1L, 2L, new BigDecimal("51.00"));

        Runnable task1 = () -> paymentService.processPayment(request1);
        Runnable task2 = () -> paymentService.processPayment(request2);
        Runnable task3 = () -> paymentService.processPayment(request3);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future<?> future1 = executor.submit(task1);
        Future<?> future2 = executor.submit(task2);
        Future<?> future3 = executor.submit(task3);

        Exception ex1 = null, ex2 = null, ex3 = null;
        try {
            future1.get();
        } catch (ExecutionException e) {
            ex1 = (Exception) e.getCause();
        }
        try {
            future2.get();
        } catch (ExecutionException e) {
            ex2 = (Exception) e.getCause();
        }
        try {
            future3.get();
        } catch (ExecutionException e) {
            ex3 = (Exception) e.getCause();
        }
        executor.shutdown();

        assertEquals(new BigDecimal("40.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("60.00"), toAccount.getBalance());
        assertTrue(ex1 == null && (ex2 != null || ex3 != null), "At least one transaction should have failed");
        verify(kafkaTemplate, times(2)).send(anyString(), anyString());
    }
}
