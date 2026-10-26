package com.bank.account.dto;

import com.bank.account.entity.Account;
import java.math.BigDecimal;

public record AccountResponseDTO(
        Long id,
        String accountNumber,
        Long customerId,
        BigDecimal balance,
        String accountType,
        String status
) {
    public static AccountResponseDTO fromEntity(Account account) {
        return new AccountResponseDTO(
                account.id,
                account.accountNumber,
                account.customerId,
                account.balance,
                account.accountType,
                account.status
        );
    }
}