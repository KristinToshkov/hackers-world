package app.IT;

import app.defenseUpgrade.model.DefenseUpgrade;
import app.defenseUpgrade.service.DefenseUpgradeService;
import app.hack.model.Hack;
import app.hack.model.HackStatus;
import app.hack.repository.HackRepository;
import app.hack.service.HackService;
import app.offenseUpgrade.model.OffenseUpgrade;
import app.offenseUpgrade.repository.OffenseUpgradeRepository;
import app.offenseUpgrade.service.OffenseUpgradeService;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionType;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HackServiceIT {

    @Autowired
    private HackService hackService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackRepository hackRepository;

    @Autowired
    private OffenseUpgradeRepository offenseUpgradeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private OffenseUpgradeService offenseUpgradeService;

    @Autowired
    private DefenseUpgradeService defenseUpgradeService;

    private User attacker;
    private User defender;

    @BeforeEach
    void setUp() {
        // Create attacker and defender
        attacker = userRepository.save(User.builder()
                .username("attacker")
                .email("attacker@example.com")
                .password("hashedpassword")
                .role(UserRole.USER)
                .credits(200.0)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        defender = userRepository.save(User.builder()
                .username("defender")
                .email("defender@example.com")
                .password("hashedpassword")
                .role(UserRole.USER)
                .credits(100.0)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());
    }

    @Test
    void testCreateNewHack_SuccessfulHack_ShouldTransferCredits() {
        // Given
        double hackAmount = 50.0;

        // When
        hackService.createNewHack(attacker, defender, hackAmount);

        // Then
        User updatedAttacker = userRepository.findById(attacker.getId()).orElseThrow();
        User updatedDefender = userRepository.findById(defender.getId()).orElseThrow();
        Hack hack = hackRepository.findAll().get(0);
        Transaction attackerTransaction = (Transaction) transactionRepository.findByUserAndDescription(updatedAttacker, "Hack").get(0);
        Transaction defenderTransaction = (Transaction) transactionRepository.findByUserAndDescription(updatedDefender, "Hack").get(0);

        assertEquals(250.0, updatedAttacker.getCredits()); // 200 + 50
        assertEquals(50.0, updatedDefender.getCredits());  // 100 - 50
        assertEquals(HackStatus.Succeeded, hack.getStatus());
        assertEquals(50.0, hack.getCredits());
        assertEquals(TransactionType.RECEIVE, attackerTransaction.getTransactionType());
        assertEquals(TransactionType.SEND, defenderTransaction.getTransactionType());
    }

    @Test
    void testCreateNewHack_DefenderHasDefense_ShouldBeDefended() {
        // Given
        defender.setDefense(attacker);
        userRepository.save(defender);

        // When
        hackService.createNewHack(attacker, defender, 50.0);

        // Then
        Hack hack = hackRepository.findAll().get(0);
        User updatedDefender = userRepository.findById(defender.getId()).orElseThrow();

        assertEquals(HackStatus.Defended, hack.getStatus());
        assertEquals(100.0, updatedDefender.getCredits()); // Credits remain unchanged
        assertEquals(200.0, attacker.getCredits()); // No transfer happened
    }

    @Test
    void testCreateNewHack_DefenderHasDefenseUpgrade_ShouldBeDefended() {
        // Given
        DefenseUpgrade defenseUpgrade = DefenseUpgrade.builder().owner(defender).uses(1).build();
        defender.setDefenseUpgrade(defenseUpgrade);
        userRepository.save(defender);

        // When
        hackService.createNewHack(attacker, defender, 50.0);

        // Then
        Hack hack = hackRepository.findAll().get(0);
        User updatedDefender = userRepository.findById(defender.getId()).orElseThrow();

        assertEquals(HackStatus.Defended, hack.getStatus());
        assertEquals(100.0, updatedDefender.getCredits()); // Credits remain unchanged
    }

    @Test
    void testCreateNewHack_AttackerHasOffenseUpgrade_ShouldIncreaseStolenCredits() {
        // Given
        OffenseUpgrade offenseUpgrade = offenseUpgradeRepository.save(
                OffenseUpgrade.builder().owner(attacker).build()
        ); // Persist the offense upgrade

        attacker.setOffenseUpgrade(offenseUpgrade);
        userRepository.save(attacker); // Update attacker with offense upgrade

        double hackAmount = 50.0;
        double expectedStolen = 50.0 * 1.5; // Assuming OFFENSE_UPGRADE_MULTIPLER = 1.5

        // When
        hackService.createNewHack(attacker, defender, hackAmount);

        // Then
        User updatedAttacker = userRepository.findById(attacker.getId()).orElseThrow();
        User updatedDefender = userRepository.findById(defender.getId()).orElseThrow();
        Hack hack = hackRepository.findAll().get(0);

        assertEquals(200.0 + expectedStolen, updatedAttacker.getCredits());
        assertEquals(100.0 - expectedStolen, updatedDefender.getCredits());
        assertEquals(HackStatus.Succeeded, hack.getStatus());
    }


    @Test
    void testCreateNewHack_DefenderHasLessCreditsThanHackAmount_ShouldStealOnlyAvailable() {
        // Given
        defender.setCredits(30.0);
        userRepository.save(defender);

        // When
        hackService.createNewHack(attacker, defender, 50.0);

        // Then
        User updatedAttacker = userRepository.findById(attacker.getId()).orElseThrow();
        User updatedDefender = userRepository.findById(defender.getId()).orElseThrow();
        Hack hack = hackRepository.findAll().get(0);

        assertEquals(200.0 + 30.0, updatedAttacker.getCredits());
        assertEquals(0.0, updatedDefender.getCredits());
        assertEquals(HackStatus.Succeeded, hack.getStatus());
        assertEquals(30.0, hack.getCredits());
    }

    @Test
    void testChangeUserDefense_ShouldSetNewDefense() {
        // When
        hackService.changeUserDefense(attacker, defender);

        // Then
        User updatedAttacker = userRepository.findById(attacker.getId()).orElseThrow();
        assertEquals(defender, updatedAttacker.getDefense());
    }

    @Test
    void testChangeUserDefense_ShouldNotSetSameUserAsDefense() {
        // When
        hackService.changeUserDefense(attacker, attacker);

        // Then
        User updatedAttacker = userRepository.findById(attacker.getId()).orElseThrow();
        assertNull(updatedAttacker.getDefense());
    }

    @Test
    void testGetUserHistory_ShouldReturnHacks() {
        // Given
        hackService.createNewHack(attacker, defender, 20.0);
        hackService.createNewHack(defender, attacker, 15.0);

        // When
        List<Hack> attackerHistory = hackService.getUserHistory(attacker);
        List<Hack> defenderHistory = hackService.getUserHistory(defender);

        // Then
        assertEquals(2, attackerHistory.size()); // Attacker was involved in 2 hacks
        assertEquals(2, defenderHistory.size()); // Defender was involved in 2 hacks
    }
}
