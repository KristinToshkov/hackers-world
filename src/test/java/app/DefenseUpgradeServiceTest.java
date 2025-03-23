package app;

import app.defenseUpgrade.model.DefenseUpgrade;
import app.defenseUpgrade.repository.DefenseUpgradeRepository;
import app.defenseUpgrade.service.DefenseUpgradeService;
import app.exception.DomainException;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefenseUpgradeServiceTest {

    private static final double DEFENSE_UPGRADE_PRICE = 200.0;

    @Mock
    private DefenseUpgradeRepository defenseUpgradeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DefenseUpgradeService defenseUpgradeService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .credits(200.0) // Enough credits
                .build();
    }

    @Test
    void shouldBuyDefenseUpgradeSuccessfully_WhenUserHasNoUpgrade() {
        when(defenseUpgradeRepository.save(any(DefenseUpgrade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        defenseUpgradeService.buyDefenseUpgrade(user);

        assertThat(user.getCredits()).isEqualTo(0); // <-- Fix this to match the correct deduction
        assertThat(user.getDefenseUpgrade()).isNotNull();
        assertThat(user.getDefenseUpgrade().getUses()).isEqualTo(1);
        verify(transactionService).createTransaction(user, DEFENSE_UPGRADE_PRICE, "Bought Defense Upgrade", TransactionType.SEND);
        verify(defenseUpgradeRepository).save(any(DefenseUpgrade.class));
        verify(userRepository).save(user);
    }


    @Test
    void shouldIncreaseUses_WhenUserAlreadyHasDefenseUpgrade() {
        DefenseUpgrade existingUpgrade = DefenseUpgrade.builder().owner(user).uses(2).build();
        user.setDefenseUpgrade(existingUpgrade);
        user.setCredits(300.0); // Ensure initial credits match test expectation

        when(defenseUpgradeRepository.save(any(DefenseUpgrade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        defenseUpgradeService.buyDefenseUpgrade(user);

        assertThat(user.getCredits()).isEqualTo(100.0); // Verify correct deduction (300 - 200)
        assertThat(user.getDefenseUpgrade().getUses()).isEqualTo(3);
        verify(transactionService).createTransaction(user, DEFENSE_UPGRADE_PRICE, "Bought Defense Upgrade", TransactionType.SEND);
        verify(defenseUpgradeRepository).save(existingUpgrade);
        verify(userRepository).save(user);
    }


    @Test
    void shouldThrowException_WhenUserHasInsufficientCredits() {
        user.setCredits(50.0); // Not enough credits

        assertThatThrownBy(() -> defenseUpgradeService.buyDefenseUpgrade(user))
                .isInstanceOf(DomainException.class)
                .hasMessage("You do not have enough credits");

        verify(transactionService, never()).createTransaction(any(), anyDouble(), anyString(), any());
        verify(defenseUpgradeRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDecreaseUsesSuccessfully() {
        DefenseUpgrade defenseUpgrade = DefenseUpgrade.builder().owner(user).uses(2).build();
        user.setDefenseUpgrade(defenseUpgrade);

        defenseUpgradeService.decreaseUses(defenseUpgrade, user);

        assertThat(defenseUpgrade.getUses()).isEqualTo(1);
        verify(defenseUpgradeRepository, never()).delete(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRemoveDefenseUpgrade_WhenUsesReachZero() {
        DefenseUpgrade defenseUpgrade = DefenseUpgrade.builder().owner(user).uses(1).build();
        user.setDefenseUpgrade(defenseUpgrade);

        defenseUpgradeService.decreaseUses(defenseUpgrade, user);

        assertThat(user.getDefenseUpgrade()).isNull();
        verify(defenseUpgradeRepository).delete(defenseUpgrade);
        verify(userRepository).save(user);
    }
}
