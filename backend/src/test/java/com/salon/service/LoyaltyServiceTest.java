package com.salon.service;

import com.salon.dto.response.LoyaltyResponse;
import com.salon.dto.response.LoyaltyTransactionResponse;
import com.salon.entity.*;
import com.salon.exception.InvalidOperationException;
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
class LoyaltyServiceTest {

    @Mock private LoyaltyRepository loyaltyRepository;
    @Mock private LoyaltyTransactionRepository transactionRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SalonOwnerRepository salonOwnerRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private WalletService walletService;

    @InjectMocks private LoyaltyServiceImpl loyaltyService;

    private Customer customer;
    private SalonOwner owner;
    private Loyalty loyalty;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").email("alice@gmail.com").build();
        owner    = SalonOwner.builder().id(2L).name("Owner").salonName("Glamour").city("Hyderabad").build();
        loyalty  = Loyalty.builder().id(1L).customer(customer).owner(owner)
                .points(200).tier(LoyaltyTier.BRONZE).build();
    }

    // ── calculateTier ─────────────────────────────────────────────────────────

    @Test
    void calculateTier_Below500_ShouldBeBronze() {
        assertEquals(LoyaltyTier.BRONZE, loyaltyService.calculateTier(0));
        assertEquals(LoyaltyTier.BRONZE, loyaltyService.calculateTier(499));
    }

    @Test
    void calculateTier_500to1499_ShouldBeSilver() {
        assertEquals(LoyaltyTier.SILVER, loyaltyService.calculateTier(500));
        assertEquals(LoyaltyTier.SILVER, loyaltyService.calculateTier(1499));
    }

    @Test
    void calculateTier_1500to2999_ShouldBeGold() {
        assertEquals(LoyaltyTier.GOLD, loyaltyService.calculateTier(1500));
        assertEquals(LoyaltyTier.GOLD, loyaltyService.calculateTier(2999));
    }

    @Test
    void calculateTier_3000AndAbove_ShouldBeDiamond() {
        assertEquals(LoyaltyTier.DIAMOND, loyaltyService.calculateTier(3000));
        assertEquals(LoyaltyTier.DIAMOND, loyaltyService.calculateTier(9999));
    }

    // ── updatePoints ──────────────────────────────────────────────────────────

    @Test
    void updatePoints_ExistingLoyalty_ShouldAddPoints() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(salonOwnerRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(loyaltyRepository.findByCustomerIdAndOwnerId(1L, 2L)).thenReturn(Optional.of(loyalty));
        when(loyaltyRepository.save(any(Loyalty.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenReturn(null);

        LoyaltyResponse response = loyaltyService.updatePoints(1L, 2L, 100);

        assertNotNull(response);
        assertEquals(300, response.getPoints());
        assertEquals(1L, response.getCustomerId());
        verify(loyaltyRepository).save(any(Loyalty.class));
    }

    @Test
    void updatePoints_NoExistingLoyalty_ShouldCreateNew() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(salonOwnerRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(loyaltyRepository.findByCustomerIdAndOwnerId(1L, 2L)).thenReturn(Optional.empty());
        when(loyaltyRepository.save(any(Loyalty.class))).thenAnswer(i -> {
            Loyalty l = i.getArgument(0);
            l.setId(99L);
            return l;
        });
        when(transactionRepository.save(any())).thenReturn(null);

        LoyaltyResponse response = loyaltyService.updatePoints(1L, 2L, 50);

        assertEquals(50, response.getPoints());
    }

    @Test
    void updatePoints_CustomerNotFound_ShouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.updatePoints(99L, 2L, 100));
    }

    @Test
    void updatePoints_OwnerNotFound_ShouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(salonOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.updatePoints(1L, 99L, 100));
    }

    // ── getLoyaltyByCustomer ──────────────────────────────────────────────────

    @Test
    void getLoyaltyByCustomer_Exists_ShouldReturnResponse() {
        when(loyaltyRepository.findByCustomerIdAndOwnerId(1L, 2L))
                .thenReturn(Optional.of(loyalty));

        LoyaltyResponse response = loyaltyService.getLoyaltyByCustomer(1L, 2L);

        assertEquals(200, response.getPoints());
        assertEquals("BRONZE", response.getTier());
    }

    @Test
    void getLoyaltyByCustomer_NotExists_ShouldReturnZeroPoints() {
        when(loyaltyRepository.findByCustomerIdAndOwnerId(1L, 2L))
                .thenReturn(Optional.empty());

        LoyaltyResponse response = loyaltyService.getLoyaltyByCustomer(1L, 2L);

        assertEquals(0, response.getPoints());
        assertEquals("BRONZE", response.getTier());
    }

    // ── getAllLoyaltyByOwner ───────────────────────────────────────────────────

    @Test
    void getAllLoyaltyByOwner_ShouldReturnList() {
        when(loyaltyRepository.findByOwnerId(2L)).thenReturn(List.of(loyalty));

        List<LoyaltyResponse> result = loyaltyService.getAllLoyaltyByOwner(2L);

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getCustomerName());
    }

    @Test
    void getAllLoyaltyByOwner_Empty_ShouldReturnEmptyList() {
        when(loyaltyRepository.findByOwnerId(2L)).thenReturn(List.of());

        assertTrue(loyaltyService.getAllLoyaltyByOwner(2L).isEmpty());
    }

    // ── getSummary ────────────────────────────────────────────────────────────

    @Test
    void getSummary_ShouldComputePointsFromTransactions() {
        LoyaltyTransaction earn1 = LoyaltyTransaction.builder()
                .id(1L).customer(customer).type("EARN").points(300).build();
        LoyaltyTransaction earn2 = LoyaltyTransaction.builder()
                .id(2L).customer(customer).type("EARN").points(200).build();
        LoyaltyTransaction redeem = LoyaltyTransaction.builder()
                .id(3L).customer(customer).type("REDEEM").points(100).build();

        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of(loyalty));
        when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(earn1, earn2, redeem));
        when(walletService.getBalance(1L)).thenReturn(new BigDecimal("10.00"));

        LoyaltyResponse response = loyaltyService.getSummary(1L);

        // totalEarned=500, totalRedeemed=100, available=400
        assertEquals(400, response.getPoints());
        assertEquals(500, response.getTotalEarned());
        assertEquals(100, response.getTotalRedeemed());
        assertEquals("SILVER", response.getTier()); // 500 earned → SILVER tier
    }

    @Test
    void getSummary_NoTransactions_ShouldReturnZeroPoints() {
        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of());
        when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());
        when(walletService.getBalance(1L)).thenReturn(BigDecimal.ZERO);

        LoyaltyResponse response = loyaltyService.getSummary(1L);

        assertEquals(0, response.getPoints());
        assertEquals("BRONZE", response.getTier());
    }

    // ── redeemPoints ──────────────────────────────────────────────────────────

    @Test
    void redeemPoints_Valid_ShouldCreditWalletAndReturnResponse() {
        LoyaltyTransaction earn = LoyaltyTransaction.builder()
                .id(1L).customer(customer).type("EARN").points(500).build();

        Wallet wallet = Wallet.builder().id(1L).customer(customer)
                .balance(new BigDecimal("50.00")).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(earn));
        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of(loyalty));
        when(loyaltyRepository.save(any())).thenReturn(loyalty);
        when(transactionRepository.save(any())).thenReturn(null);
        when(walletService.credit(any(), any(), anyString(), anyString())).thenReturn(wallet);
        when(walletService.getBalance(1L)).thenReturn(new BigDecimal("50.00"));

        LoyaltyResponse response = loyaltyService.redeemPoints(1L, 100);

        assertNotNull(response);
        verify(walletService).credit(eq(customer), eq(new BigDecimal("10.00")),
                eq("points_redemption"), anyString());
    }

    @Test
    void redeemPoints_BelowMinimum_ShouldThrowInvalidOperationException() {
        assertThrows(InvalidOperationException.class,
                () -> loyaltyService.redeemPoints(1L, 50));
    }

    @Test
    void redeemPoints_InsufficientPoints_ShouldThrowInvalidOperationException() {
        LoyaltyTransaction earn = LoyaltyTransaction.builder()
                .id(1L).customer(customer).type("EARN").points(50).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(earn));

        assertThrows(InvalidOperationException.class,
                () -> loyaltyService.redeemPoints(1L, 100));
    }

    @Test
    void redeemPoints_CustomerNotFound_ShouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.redeemPoints(99L, 100));
    }

    // ── getTransactions ───────────────────────────────────────────────────────

    @Test
    void getTransactions_ShouldReturnMappedList() {
        LoyaltyTransaction t = LoyaltyTransaction.builder()
                .id(1L).customer(customer).type("EARN").points(100)
                .description("Earned for Haircut").build();

        when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(t));

        List<LoyaltyTransactionResponse> result = loyaltyService.getTransactions(1L);

        assertEquals(1, result.size());
        assertEquals("EARN", result.get(0).getType());
        assertEquals(100, result.get(0).getPoints());
        assertEquals("Earned for Haircut", result.get(0).getDescription());
    }

    // ── awardPointsForProductPurchase ─────────────────────────────────────────

    @Test
    void awardPointsForProductPurchase_100Rupees_ShouldAward10Points() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of(loyalty));
        when(loyaltyRepository.save(any())).thenReturn(loyalty);
        when(transactionRepository.save(any())).thenReturn(null);

        loyaltyService.awardPointsForProductPurchase(1L, new BigDecimal("100.00"));

        verify(transactionRepository).save(argThat(t ->
                t instanceof LoyaltyTransaction &&
                ((LoyaltyTransaction) t).getPoints() == 10));
    }

    @Test
    void awardPointsForProductPurchase_LessThan100_ShouldNotAwardPoints() {
        loyaltyService.awardPointsForProductPurchase(1L, new BigDecimal("50.00"));

        verify(transactionRepository, never()).save(any());
        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void awardPointsForProductPurchase_500Rupees_ShouldAward50Points() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(loyaltyRepository.findByCustomerId(1L)).thenReturn(List.of(loyalty));
        when(loyaltyRepository.save(any())).thenReturn(loyalty);
        when(transactionRepository.save(any())).thenReturn(null);

        loyaltyService.awardPointsForProductPurchase(1L, new BigDecimal("500.00"));

        verify(transactionRepository).save(argThat(t ->
                t instanceof LoyaltyTransaction &&
                ((LoyaltyTransaction) t).getPoints() == 50));
    }
}
