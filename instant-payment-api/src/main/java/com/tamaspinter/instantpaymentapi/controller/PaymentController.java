package com.tamaspinter.instantpaymentapi.controller;

import com.tamaspinter.instantpaymentapi.dto.PaymentRequest;
import com.tamaspinter.instantpaymentapi.entity.PaymentTransaction;
import com.tamaspinter.instantpaymentapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments API", description = "Endpoints for managing payments")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Processes a payment transaction between two accounts.
     *
     * @param request The payment request details.
     * @return Payment transaction details if successful.
     */
    @Operation(
            summary = "Send a payment",
            description = "Processes a payment between two accounts and returns transaction details.",
            operationId = "sendPayment"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Payment processed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentTransaction.class), examples = @ExampleObject(
                            name = "Success Example",
                            value = "{ \"id\": \"1\", \"fromAccountId\": \"1\", \"toAccountId\": \"2\", \"amount\": 10.50, \"createdAt\": \"2025-03-07T17:56:56.828359453\" }"))
            ),
            @ApiResponse(
                    responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Invalid Request Example",
                            value = "{ \"error\": \"Amount must be positive\" }"))),
            @ApiResponse(
                    responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Account Not Found Example",
                            value = "{ \"error\": \"Account 1 not found\" }"))),
            @ApiResponse(
                    responseCode = "500", description = "Database error or internal server error",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Database Error Example",
                            value = "{ \"error\": \"Database error\" }")))
    })
    @PostMapping
    public ResponseEntity<?> sendPayment(@RequestBody
                                         @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                 description = "Payment request details",
                                                 required = true,
                                                 content = @Content(mediaType = "application/json", examples = @ExampleObject(
                                                                 name = "Payment Request Example",
                                                                 value = "{ \"fromAccount\": \"1\", \"toAccount\": \"2\", \"amount\": 10.50 }")))
                                         PaymentRequest request) {
        try {
            PaymentTransaction tx = paymentService.processPayment(request);
            return ResponseEntity.ok(tx);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (DataAccessResourceFailureException e) {
            return ResponseEntity.internalServerError().body("Database error");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
