package com.salon.service;

import com.salon.entity.Customer;
import com.salon.entity.Wallet;
import com.salon.entity.WalletTransaction;
import com.salon.exception.ValidationException;
import com.salon.repository.WalletRepository;
import com.salon.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks private WalletService walletService;

    private Customer customer;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").build();
        wallet = Wallet.builder().id(1L).customer(customer)
                .balance(new BigDecimal("500.00")).build();
    }

    // ── credit ────────────────────────────────────────────────────────────────

    @Test
    void credit_ExistingWallet_ShouldAddBalance() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any())).thenReturn(null);

        Wallet result = walletService.credit(customer, new BigDecimal("200.00"),
                "refund", "Test credit");

        assertEquals(new BigDecimal("700.00"), result.getBalance());
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void credit_NoWalletYet_ShouldCreateAndCredit() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any())).thenReturn(null);

        Wallet result = walletService.credit(customer, new BigDecimal("100.00"),
                "signup_bonus", "Welcome bonus");

        assertEquals(new BigDecimal("100.00"), result.getBalance());
    }

    // ── getBalance ────────────────────────────────────────────────────────────

    @Test
    void getBalance_WalletExists_ShouldReturnBalance() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(wallet));

        BigDecimal balance = walletService.getBalance(1L);

        assertEquals(new BigDecimal("500.00"), balance);
    }

    @Test
    void getBalance_NoWallet_ShouldReturnZero() {
        when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.empty());

        BigDecimal balance = walletService.getBalance(99L);

        assertEquals(BigDecimal.ZERO, balance);
    }

    // ── debit ─────────────────────────────────────────────────────────────────

    @Test
    void debit_SufficientBalance_ShouldSubtractAndSave() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any())).thenReturn(null);

        Wallet result = walletService.debit(customer, new BigDecimal("200.00"),
                "payment", "Service payment");

        assertEquals(new BigDecimal("300.00"), result.getBalance());
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void debit_InsufficientBalance_ShouldThrowValidationException() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(wallet));

        assertThrows(ValidationException.class,
                () -> walletService.debit(customer, new BigDecimal("600.00"),
                        "payment", "Too much"));
    }

    @Test
    void debit_NoWallet_ShouldThrowValidationException() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> walletService.debit(customer, new BigDecimal("100.00"),
                        "payment", "No wallet"));
    }

    @Test
    void debit_ExactBalance_ShouldResultInZero() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any())).thenReturn(null);

        Wallet result = walletService.debit(customer, new BigDecimal("500.00"),
                "payment", "Exact amount");

        assertEquals(BigDecimal.ZERO, result.getBalance().stripTrailingZeros());
    }

    // ── getTransactions ───────────────────────────────────────────────────────

    @Test
    void getTransactions_ShouldReturnOrderedList() {
        WalletTransaction t1 = WalletTransaction.builder()
                .id(1L).customer(customer).type("credit")
                .amount(new BigDecimal("100.00")).build();
        WalletTransaction t2 = WalletTransaction.builder()
                .id(2L).customer(customer).type("debit")
                .amount(new BigDecimal("50.00")).build();

        when(walletTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(t2, t1));

        List<WalletTransaction> txns = walletService.getTransactions(1L);

        assertEquals(2, txns.size());
        assertEquals("debit", txns.get(0).getType());
    }
}
