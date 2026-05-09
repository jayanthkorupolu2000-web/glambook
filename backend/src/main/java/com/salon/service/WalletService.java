package com.salon.service;

import com.salon.entity.Customer;
import com.salon.entity.Wallet;
import com.salon.entity.WalletTransaction;
import com.salon.repository.WalletRepository;
import com.salon.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * Credit the customer's wallet. Creates the wallet row if it doesn't exist yet.
     * Also inserts a wallet_transactions record.
     */
    @Transactional
    public Wallet credit(Customer customer, BigDecimal amount, String source, String description) {
        Wallet wallet = walletRepository.findByCustomerId(customer.getId())
                .orElse(Wallet.builder().customer(customer).balance(BigDecimal.ZERO).build());

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .customer(customer)
                .type("credit")
                .amount(amount)
                .source(source)
                .description(description)
                .build());

        log.info("Credited ₹{} to wallet of customer {} (source={})", amount, customer.getId(), source);
        return saved;
    }

    /** Get current wallet balance for a customer (returns 0 if no wallet yet). */
    public BigDecimal getBalance(Long customerId) {
        return walletRepository.findByCustomerId(customerId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Debit the customer's wallet. Throws ValidationException if balance is insufficient.
     */
    @Transactional
    public Wallet debit(Customer customer, BigDecimal amount, String source, String description) {
        Wallet wallet = walletRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new com.salon.exception.ValidationException("No wallet found for customer"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new com.salon.exception.ValidationException(
                    "Insufficient wallet balance. Available: ₹" + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        Wallet saved = walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .customer(customer)
                .type("debit")
                .amount(amount)
                .source(source)
                .description(description)
                .build());

        log.info("Debited ₹{} from wallet of customer {} (source={})", amount, customer.getId(), source);
        return saved;
    }

    /** Full transaction history, newest first. */
    public List<WalletTransaction> getTransactions(Long customerId) {
        return walletTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
