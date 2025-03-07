package com.tamaspinter.instantpaymentapi.controller;

import com.tamaspinter.instantpaymentapi.dto.AccountRequest;
import com.tamaspinter.instantpaymentapi.dto.DepositRequest;
import com.tamaspinter.instantpaymentapi.entity.Account;
import com.tamaspinter.instantpaymentapi.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;

@Tag(name = "Account API", description = "Endpoints for managing accounts")
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * Creates a new account.
     *
     * @param request The request body containing account details.
     * @return The created account object.
     */
    @Operation(
            summary = "Create a new account",
            description = "Creates a new account and returns its details",
            operationId = "createAccount"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Account successfully created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class), examples = @ExampleObject(
                            name = "Success Example",
                            value = "{\n" +
                                    "    \"id\": 5,\n" +
                                    "    \"balance\": 0,\n" +
                                    "    \"accountName\": \"Savings account\",\n" +
                                    "    \"ownerName\": \"Tamas Pinter\",\n" +
                                    "    \"version\": 0\n" +
                                    "}"))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Invalid Input Example",
                            value = "{\n" +
                                    "    \"timestamp\": \"2025-03-07T16:21:38.166+00:00\",\n" +
                                    "    \"status\": 400,\n" +
                                    "    \"error\": \"Bad Request\",\n" +
                                    "    \"path\": \"/api/accounts\"\n" +
                                    "}"))),
            @ApiResponse(
                    responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody AccountRequest request) {
        Account account = accountService.createAccount(request);
        return ResponseEntity.ok(account);
    }

    /**
     * Returns account details using an ID.
     *
     * @param accountId The ID of the account.
     * @return Account details if found.
     */
    @Operation(
            summary = "Get account by ID",
            description = "Returns account details using an ID.",
            operationId = "getAccountById"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class), examples = @ExampleObject(
                            name = "Account Found Example",
                            value = "{\n" +
                                    "    \"id\": 1,\n" +
                                    "    \"balance\": 162.00,\n" +
                                    "    \"accountName\": \"Savings\",\n" +
                                    "    \"ownerName\": \"Tamas Pinter\",\n" +
                                    "    \"version\": 11\n" +
                                    "}"))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "Account Not Found Example",
                                    value = "Account with ID 1 not found"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable("accountId") Long accountId) {
        try {
            Account account = accountService.getAccountById(accountId);
            return ResponseEntity.ok(account);
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account with ID " + accountId + " not found");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Deposits money into an account.
     *
     * @param depositRequest The deposit details.
     * @return The updated account with the new balance.
     */
    @Operation(
            summary = "Deposit to an account",
            description = "Adds the specified amount to the account balance.",
            operationId = "depositToAccount"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class))),
            @ApiResponse(responseCode = "404", description = "Account with ID <accountId> not found"),
            @ApiResponse(responseCode = "400", description = "Invalid deposit amount"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest depositRequest) {
        try {
            Account account = accountService.deposit(depositRequest);
            return ResponseEntity.ok(account);
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account with ID " + depositRequest.accountId() + " not found");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid deposit amount");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
