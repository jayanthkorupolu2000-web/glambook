package com.salon.service;

import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import com.salon.service.impl.LoyaltyServiceImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyProductPurchaseTest {

    @Mock private LoyaltyRepository loyaltyRepository;
    @Mock private LoyaltyTransactionRepository transactionRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SalonOwnerRepository salonOwnerRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private WalletService walletService;

    @InjectMocks
    private LoyaltyServiceImpl loyaltyService;

    private Customer customer;
    private Loyalty loyalty;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").email("alice@test.com").build();
        loyalty = Loyalty.builder().id(1L).customer(customer).points(200).build();
    }

    @Test
    void awardPointsForProductPurchase_100Rupees_Awards10Points() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of(loyalty));
        when(loyaltyRepository.save(any())).thenReturn(loyalty);
        when(transactionRepository.save(any())).thenReturn(null);

        loyaltyService.awardPointsForProductPurchase(1L, BigDecimal.valueOf(100));

        verify(loyaltyRepository).save(argThat(l -> l.getPoints() == 210)); // 200 + 10
        verify(transactionRepository).save(argThat(t ->
                "EARN".equals(t.getType()) && t.getPoints() == 10));
    }

    @Test
    void awardPointsForProductPurchase_450Rupees_Awards40Points() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of(loyalty));
        when(loyaltyRepository.save(any())).thenReturn(loyalty);
        when(transactionRepository.save(any())).thenReturn(null);

        loyaltyService.awardPointsForProductPurchase(1L, BigDecimal.valueOf(450));

        // 450 / 100 = 4 (floor) * 10 = 40 points
        verify(loyaltyRepository).save(argThat(l -> l.getPoints() == 240));
        verify(transactionRepository).save(argThat(t -> t.getPoints() == 40));
    }

    @Test
    void awardPointsForProductPurchase_LessThan100Rupees_AwardsZeroPoints() {
        // 99 / 100 = 0 points — no save should happen
        loyaltyService.awardPointsForProductPurchase(1L, BigDecimal.valueOf(99));

        verify(loyaltyRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void awardPointsForProductPurchase_CustomerNotFound_ThrowsResourceNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.awardPointsForProductPurchase(99L, BigDecimal.valueOf(500)));
    }

    @Test
    void awardPointsForProductPurchase_NoExistingLoyalty_OnlyRecordsTransaction() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of()); // no loyalty record
        when(transactionRepository.save(any())).thenReturn(null);

        loyaltyService.awardPointsForProductPurchase(1L, BigDecimal.valueOf(200));

        // No loyalty record to update, but transaction should still be saved
        verify(loyaltyRepository, never()).save(any());
        verify(transactionRepository).save(argThat(t -> t.getPoints() == 20));
    }
}
