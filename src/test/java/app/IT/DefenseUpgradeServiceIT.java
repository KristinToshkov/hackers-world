package app.IT;

import app.defenseUpgrade.model.DefenseUpgrade;
import app.defenseUpgrade.repository.DefenseUpgradeRepository;
import app.defenseUpgrade.service.DefenseUpgradeService;
import app.transaction.repository.TransactionRepository;
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

import static app.constant.Constants.DEFENSE_UPGRADE_PRICE;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // Ensure we use the test profile
@Transactional  // Rollback changes after each test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)  // Reset context between tests
class DefenseUpgradeServiceIT {

    @Autowired
    private DefenseUpgradeService defenseUpgradeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DefenseUpgradeRepository defenseUpgradeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User user;

    @BeforeEach
    void setUp() {
        // Create a user with enough credits
        user = User.builder()
                .username("testUser")
                .email("test@example.com")
                .password("password")
                .role(UserRole.USER)
                .credits(400.0) // Enough to buy at least one defense upgrade
                .createdOn(LocalDateTime.now())
                .build();
        userRepository.save(user);
    }

    @Test
    void testBuyDefenseUpgrade_ShouldDeductCreditsAndCreateDefenseUpgrade() {
        // When
        defenseUpgradeService.buyDefenseUpgrade(user);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        DefenseUpgrade defenseUpgrade = defenseUpgradeRepository.findById(updatedUser.getDefenseUpgrade().getId()).orElseThrow();

        assertNotNull(updatedUser.getDefenseUpgrade()); // Check upgrade was added
        assertEquals(1, defenseUpgrade.getUses()); // Initial uses should be 1

        // Check transaction was created
        assertEquals(1, transactionRepository.count());
    }

    @Test
    void testBuyDefenseUpgrade_UserAlreadyHasUpgrade_ShouldIncreaseUses() {
        // Given
        defenseUpgradeService.buyDefenseUpgrade(user); // First purchase
        defenseUpgradeService.buyDefenseUpgrade(user); // Second purchase

        // When
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        DefenseUpgrade defenseUpgrade = defenseUpgradeRepository.findById(updatedUser.getDefenseUpgrade().getId()).orElseThrow();

        // Then
        assertNotNull(updatedUser.getDefenseUpgrade()); // Still has upgrade
        assertEquals(2, defenseUpgrade.getUses()); // Uses increased to 2
    }

    @Test
    void testDecreaseUses_ShouldRemoveDefenseUpgradeIfUsesReachZero() {
        // Given
        defenseUpgradeService.buyDefenseUpgrade(user);
        DefenseUpgrade defenseUpgrade = user.getDefenseUpgrade();
        defenseUpgrade.setUses(1); // Ensure only 1 use remains
        defenseUpgradeRepository.save(defenseUpgrade);

        // When
        defenseUpgradeService.decreaseUses(defenseUpgrade, user);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertNull(updatedUser.getDefenseUpgrade()); // Upgrade should be removed
        assertEquals(0, defenseUpgradeRepository.count()); // No upgrades in DB
    }

    @Test
    void testDecreaseUses_ShouldReduceUsesButNotRemoveIfUsesRemain() {
        // Given
        defenseUpgradeService.buyDefenseUpgrade(user);
        defenseUpgradeService.buyDefenseUpgrade(user); // Now has 2 uses

        DefenseUpgrade defenseUpgrade = user.getDefenseUpgrade();
        defenseUpgradeRepository.save(defenseUpgrade);

        // When
        defenseUpgradeService.decreaseUses(defenseUpgrade, user);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        DefenseUpgrade updatedDefenseUpgrade = defenseUpgradeRepository.findById(defenseUpgrade.getId()).orElseThrow();

        // Then
        assertNotNull(updatedUser.getDefenseUpgrade()); // Upgrade still exists
        assertEquals(1, updatedDefenseUpgrade.getUses()); // Uses reduced by 1
    }
}
