package app.IT;

import app.exception.DomainException;
import app.offenseUpgrade.model.OffenseUpgrade;
import app.offenseUpgrade.repository.OffenseUpgradeRepository;
import app.offenseUpgrade.service.OffenseUpgradeService;
import app.transaction.model.Transaction;
import app.transaction.repository.TransactionRepository;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OffenseUpgradeServiceIT {

    @Autowired
    private OffenseUpgradeService offenseUpgradeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OffenseUpgradeRepository offenseUpgradeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    private static final double OFFENSE_UPGRADE_PRICE = 250.0;
    private static final double OFFENSE_UPGRADE_MULTIPLER = 1.5;

    private User user;

    @BeforeEach
    void setUp() {
        // Create a test user
        user = userRepository.save(User.builder()
                .username("testUser")
                .email("test@example.com")
                .password("hashedpassword")
                .role(UserRole.USER)
                .credits(300.00)  // Enough to buy upgrade
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());
    }

    @Test
    void testCreateOffenseUpgrade_ShouldDeductCreditsAndSaveUpgrade() {
        // When
        offenseUpgradeService.createOffenseUpgrade(user);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        OffenseUpgrade upgrade = offenseUpgradeRepository.findById(updatedUser.getOffenseUpgrade().getId()).orElse(null);
        Transaction transaction = transactionRepository.findAll().get(0);

        assertNotNull(upgrade);
        assertEquals(50.0, updatedUser.getCredits()); // 300 - 250 = 50
        assertEquals(OFFENSE_UPGRADE_PRICE, transaction.getCredits());
        assertEquals("Bough Offense Upgrade", transaction.getDescription());
    }

    @Test
    void testCreateOffenseUpgrade_ShouldThrowException_WhenAlreadyOwned() {
        // Given
        offenseUpgradeService.createOffenseUpgrade(user); // First purchase

        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> {
            offenseUpgradeService.createOffenseUpgrade(user); // Second purchase
        });

        assertEquals("Already owned!", exception.getMessage());
    }

    @Test
    void testCreateOffenseUpgrade_ShouldThrowException_WhenNotEnoughCredits() {
        // Given
        user.setCredits(30.0); // Not enough for the upgrade
        userRepository.save(user);

        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> {
            offenseUpgradeService.createOffenseUpgrade(user);
        });

        assertEquals("Not enough credits", exception.getMessage());
    }

    @Test
    void testCalculateCredits_ShouldApplyMultiplierCorrectly() {
        // Given
        double baseCredits = 100.0;

        // When
        double upgradedCredits = offenseUpgradeService.calculateCredits(baseCredits);

        // Then
        assertEquals(150.0, upgradedCredits);
    }
}
