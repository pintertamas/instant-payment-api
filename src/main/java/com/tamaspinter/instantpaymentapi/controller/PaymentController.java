package com.tamaspinter.instantpaymentapi.controller;

import com.tamaspinter.instantpaymentapi.dto.PaymentRequest;
import com.tamaspinter.instantpaymentapi.entity.PaymentTransaction;
import com.tamaspinter.instantpaymentapi.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> sendPayment(@RequestBody PaymentRequest request) {
        try {
            PaymentTransaction tx = paymentService.processPayment(request);
            return ResponseEntity.ok(tx);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
