package com.salon.service.impl;

import com.salon.dto.response.LoyaltyResponse;
import com.salon.dto.response.LoyaltyTransactionResponse;
import com.salon.entity.*;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import com.salon.service.LoyaltyService;
import com.salon.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyRepository loyaltyRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final AppointmentRepository appointmentRepository;
    private final WalletService walletService;

    private static final int MIN_REDEEM_POINTS  = 100;
    private static final int REDEEM_MULTIPLE    = 100;
    private static final int POINTS_PER_RUPEE   = 10;   // 100 pts = ₹10  →  10 pts per ₹1

    // ── Tier thresholds ───────────────────────────────────────────────────────
    @Override
    public LoyaltyTier calculateTier(int points) {
        if (points >= 3000) return LoyaltyTier.PLATINUM;
        if (points >= 1500) return LoyaltyTier.GOLD;
        if (points >= 500)  return LoyaltyTier.SILVER;
        return LoyaltyTier.BRONZE;
    }

    // ── Award points for product purchase ─────────────────────────────────────
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void awardPointsForProductPurchase(Long customerId, java.math.BigDecimal amountSpent) {
        int points = amountSpent.divide(java.math.BigDecimal.valueOf(100), 0,
                java.math.RoundingMode.DOWN).multiply(java.math.BigDecimal.TEN).intValue();
        if (points <= 0) return;

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Use the first loyalty record (or create a standalone one with no owner)
        List<Loyalty> records = loyaltyRepository.findByCustomerId(customerId);
        if (!records.isEmpty()) {
            Loyalty loyalty = records.get(0);
            loyalty.setPoints(loyalty.getPoints() + points);
            loyalty.setUpdatedAt(java.time.LocalDateTime.now());
            loyaltyRepository.save(loyalty);
        }
        // Always record the transaction regardless of existing loyalty record
        transactionRepository.save(LoyaltyTransaction.builder()
                .customer(customer)
                .type("EARN")
                .points(points)
                .description("Earned " + points + " pts for product purchase (₹" + amountSpent + ")")
                .build());

        log.info("Awarded {} loyalty points to customer {} for product purchase of ₹{}",
                points, customerId, amountSpent);
    }

    // ── Award points when payment is completed ────────────────────────────────
    @Override
    @Transactional
    public void awardPointsOnAppointmentCompletion(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId).orElse(null);
        if (appt == null) return;

        Long customerId = appt.getCustomer().getId();
        Long ownerId    = appt.getProfessional().getSalonOwner().getId();

        // Calculate points: 1 point per ₹10 of service price, minimum 5
        int earned = 5;
        if (appt.getService() != null && appt.getService().getPrice() != null) {
            BigDecimal price = appt.getService().getPrice();
            earned = Math.max(5, price.intValue() / 10);   // 1 pt per ₹10
        }

        Customer customer = appt.getCustomer();
        SalonOwner owner  = appt.getProfessional().getSalonOwner();

        Loyalty loyalty = loyaltyRepository.findByCustomerIdAndOwnerId(customerId, ownerId)
                .orElse(Loyalty.builder().customer(customer).owner(owner).points(0).build());

        loyalty.setPoints(loyalty.getPoints() + earned);
        // Tier is based on lifetime earned — fetch total earned from transactions
        int lifetimeEarned = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().filter(t -> "EARN".equals(t.getType())).mapToInt(LoyaltyTransaction::getPoints).sum()
                + earned; // include the current earn before it's saved
        loyalty.setTier(calculateTier(lifetimeEarned));
        loyalty.setUpdatedAt(LocalDateTime.now());
        loyaltyRepository.save(loyalty);

        // Record transaction
        String serviceName = appt.getService() != null ? appt.getService().getName() : "Service";
        String price = appt.getService() != null && appt.getService().getPrice() != null
                ? "₹" + appt.getService().getPrice() : "";
        transactionRepository.save(LoyaltyTransaction.builder()
                .customer(customer)
                .type("EARN")
                .points(earned)
                .description("Earned for " + serviceName + (price.isEmpty() ? "" : " (" + price + ")"))
                .appointment(appt)
                .build());

        log.info("Awarded {} points to customer {} for appointment {}", earned, customerId, appointmentId);
    }

    // ── Redeem points ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public LoyaltyResponse redeemPoints(Long customerId, int pointsToRedeem) {
        if (pointsToRedeem < MIN_REDEEM_POINTS)
            throw new InvalidOperationException("Minimum redemption is " + MIN_REDEEM_POINTS + " points");
        if (pointsToRedeem % REDEEM_MULTIPLE != 0)
            throw new InvalidOperationException("Points must be redeemed in multiples of " + REDEEM_MULTIPLE);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        List<Loyalty> records = loyaltyRepository.findByCustomerId(customerId);
        int total = records.stream().mapToInt(Loyalty::getPoints).sum();

        if (pointsToRedeem > total)
            throw new InvalidOperationException("Insufficient points. You have " + total + " pts.");

        // Deduct points across loyalty records (spread if needed)
        int remaining = pointsToRedeem;
        for (Loyalty l : records) {
            if (remaining <= 0) break;
            int deduct = Math.min(l.getPoints(), remaining);
            l.setPoints(l.getPoints() - deduct);
            l.setUpdatedAt(LocalDateTime.now());
            loyaltyRepository.save(l);
            remaining -= deduct;
        }

        // Calculate rupee amount: 100 pts = ₹10
        BigDecimal creditAmount = BigDecimal.valueOf((long) pointsToRedeem / REDEEM_MULTIPLE * 10);

        // Record loyalty transaction
        transactionRepository.save(LoyaltyTransaction.builder()
                .customer(customer)
                .type("REDEEM")
                .points(pointsToRedeem)
                .description("Redeemed " + pointsToRedeem + " pts → ₹" + creditAmount + " added to wallet")
                .build());

        // Credit wallet and record wallet transaction
        Wallet wallet = walletService.credit(
                customer,
                creditAmount,
                "points_redemption",
                "Points redemption: " + pointsToRedeem + " pts → ₹" + creditAmount
        );

        log.info("Customer {} redeemed {} points → ₹{} credited to wallet (new balance: ₹{})",
                customerId, pointsToRedeem, creditAmount, wallet.getBalance());

        LoyaltyResponse response = getSummary(customerId);
        response.setLastCreditedAmount(creditAmount);
        response.setWalletBalance(wallet.getBalance());
        return response;
    }

    // ── Customer summary (all owners combined) ────────────────────────────────
    @Override
    public LoyaltyResponse getSummary(Long customerId) {
        List<Loyalty> records = loyaltyRepository.findByCustomerId(customerId);
        int total = records.stream().mapToInt(Loyalty::getPoints).sum();
        LoyaltyTier tier = calculateTier(total);

        List<LoyaltyTransaction> txns = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        int totalEarned   = txns.stream().filter(t -> "EARN".equals(t.getType())).mapToInt(LoyaltyTransaction::getPoints).sum();
        int totalRedeemed = txns.stream().filter(t -> "REDEEM".equals(t.getType())).mapToInt(LoyaltyTransaction::getPoints).sum();

        LoyaltyResponse res = new LoyaltyResponse();
        res.setCustomerId(customerId);
        res.setPoints(total);
        res.setTier(calculateTier(totalEarned).name());   // tier based on lifetime earned, not current balance
        res.setTotalEarned(totalEarned);
        res.setTotalRedeemed(totalRedeemed);
        res.setTierBenefits(tierBenefits(calculateTier(totalEarned)));
        res.setEarlyAccessServices(earlyAccessServices(calculateTier(totalEarned)));
        res.setWalletBalance(walletService.getBalance(customerId));
        return res;
    }

    // ── Transaction history ───────────────────────────────────────────────────
    @Override
    public List<LoyaltyTransactionResponse> getTransactions(Long customerId) {
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toTxnResponse).collect(Collectors.toList());
    }

    // ── Owner-scoped methods (unchanged) ──────────────────────────────────────
    @Override
    @Transactional
    public LoyaltyResponse updatePoints(Long customerId, Long ownerId, Integer pointsToAdd) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        SalonOwner owner = salonOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + ownerId));

        Loyalty loyalty = loyaltyRepository.findByCustomerIdAndOwnerId(customerId, ownerId)
                .orElse(Loyalty.builder().customer(customer).owner(owner).points(0).build());

        loyalty.setPoints(loyalty.getPoints() + pointsToAdd);
        loyalty.setTier(calculateTier(loyalty.getPoints()));
        loyalty.setUpdatedAt(LocalDateTime.now());
        loyaltyRepository.save(loyalty);
        return getSummary(customerId);
    }

    @Override
    public LoyaltyResponse getLoyaltyByCustomer(Long customerId, Long ownerId) {
        return loyaltyRepository.findByCustomerIdAndOwnerId(customerId, ownerId)
                .map(l -> {
                    LoyaltyResponse r = new LoyaltyResponse();
                    r.setId(l.getId());
                    r.setCustomerId(l.getCustomer().getId());
                    r.setCustomerName(l.getCustomer().getName());
                    r.setOwnerId(l.getOwner().getId());
                    r.setPoints(l.getPoints());
                    r.setTier(l.getTier().name());
                    return r;
                })
                .orElseGet(() -> {
                    LoyaltyResponse r = new LoyaltyResponse();
                    r.setCustomerId(customerId);
                    r.setOwnerId(ownerId);
                    r.setPoints(0);
                    r.setTier(LoyaltyTier.BRONZE.name());
                    return r;
                });
    }

    @Override
    public List<LoyaltyResponse> getAllLoyaltyByOwner(Long ownerId) {
        return loyaltyRepository.findByOwnerId(ownerId).stream()
                .map(l -> {
                    LoyaltyResponse r = new LoyaltyResponse();
                    r.setId(l.getId());
                    r.setCustomerId(l.getCustomer().getId());
                    r.setCustomerName(l.getCustomer().getName());
                    r.setOwnerId(l.getOwner().getId());
                    r.setPoints(l.getPoints());
                    r.setTier(l.getTier().name());
                    return r;
                }).collect(Collectors.toList());
    }

    // ── Tier metadata ─────────────────────────────────────────────────────────
    private List<String> tierBenefits(LoyaltyTier tier) {
        return switch (tier) {
            case BRONZE -> List.of(
                "Earn 1 point per ₹10 spent",
                "Redeem 100 pts = ₹10 discount",
                "Birthday bonus points"
            );
            case SILVER -> List.of(
                "All Bronze benefits",
                "5% discount on all bookings",
                "Priority appointment slots",
                "Free consultation once a month"
            );
            case GOLD -> List.of(
                "All Silver benefits",
                "10% discount on all bookings",
                "Complimentary hair wash with every haircut",
                "Early access to new services",
                "Dedicated customer support"
            );
            case PLATINUM -> List.of(
                "All Gold benefits",
                "15% discount on all bookings",
                "Free monthly treatment",
                "VIP early access to all new services",
                "Exclusive Platinum-only promotions",
                "Personal style consultant"
            );
        };
    }

    private List<String> earlyAccessServices(LoyaltyTier tier) {
        return switch (tier) {
            case BRONZE -> List.of();
            case SILVER -> List.of("Keratin Treatment", "Scalp Therapy");
            case GOLD   -> List.of("Keratin Treatment", "Scalp Therapy", "Bridal Makeup Package", "Anti-Aging Facial");
            case PLATINUM -> List.of("Keratin Treatment", "Scalp Therapy", "Bridal Makeup Package",
                    "Anti-Aging Facial", "Luxury Spa Package", "Celebrity Styling Session");
        };
    }

    private LoyaltyTransactionResponse toTxnResponse(LoyaltyTransaction t) {
        LoyaltyTransactionResponse r = new LoyaltyTransactionResponse();
        r.setId(t.getId());
        r.setType(t.getType());
        r.setPoints(t.getPoints());
        r.setDescription(t.getDescription());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }
}
