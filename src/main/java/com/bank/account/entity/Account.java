package com.bank.account.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account extends PanacheEntity {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Column(name = "account_number", nullable = false, unique = true)
    public String accountNumber;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Column(name = "customer_id", nullable = false)
    public Long customerId;

    @NotNull(message = "El saldo es obligatorio")
    @Min(value = 0, message = "El saldo no puede ser negativo")
    @Column(nullable = false)
    public BigDecimal balance;

    @NotBlank(message = "El tipo de cuenta es obligatorio")
    @Column(name = "account_type", nullable = false)
    public String accountType; // Ej: "CHECKING", "SAVINGS"

    @NotBlank(message = "El estado es obligatorio")
    @Column(nullable = false)
    public String status; // Ej: "ACTIVE", "INACTIVE"
}