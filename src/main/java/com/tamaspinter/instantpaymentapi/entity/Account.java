package com.tamaspinter.instantpaymentapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column
    private String accountName;

    @Column(nullable = false)
    private String ownerName;

    @Version
    private Long version;

    public Account(BigDecimal balance) {
        this.balance = balance;
    }
}
