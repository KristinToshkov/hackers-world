package app;

import app.defenseUpgrade.model.DefenseUpgrade;
import app.defenseUpgrade.service.DefenseUpgradeService;
import app.hack.model.Hack;
import app.hack.model.HackStatus;
import app.hack.repository.HackRepository;
import app.hack.service.HackService;
import app.offenseUpgrade.model.OffenseUpgrade;
import app.offenseUpgrade.service.OffenseUpgradeService;
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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HackServiceTest {

    @Mock
    private HackRepository hackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OffenseUpgradeService offenseUpgradeService;

    @Mock
    private DefenseUpgradeService defenseUpgradeService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private HackService hackService;

    private User attacker;
    private User defender;

    @BeforeEach
    void setUp() {
        attacker = User.builder()
                .id(UUID.randomUUID())
                .username("attackerUser")
                .credits(500.0)
                .build();

        defender = User.builder()
                .id(UUID.randomUUID())
                .username("defenderUser")
                .credits(300.0)
                .build();
    }

    @Test
    void shouldCreateNewHackAsDefended_WhenDefenderHasSetDefense() {
        defender.setDefense(attacker);

        hackService.createNewHack(attacker, defender, 100.0);

        verify(hackRepository).save(argThat(hack -> hack.getStatus() == HackStatus.Defended));
        verifyNoInteractions(transactionService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldCreateNewHackAsDefended_WhenDefenderHasDefenseUpgrade() {
        DefenseUpgrade defenseUpgrade = DefenseUpgrade.builder().owner(defender).uses(1).build();
        defender.setDefenseUpgrade(defenseUpgrade);

        hackService.createNewHack(attacker, defender, 100.0);

        verify(defenseUpgradeService).decreaseUses(defenseUpgrade, defender);
        verify(hackRepository).save(argThat(hack -> hack.getStatus() == HackStatus.Defended));
        verifyNoInteractions(transactionService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldCreateNewHackAsSucceeded_WhenAttackSucceeds() {
        when(hackRepository.save(any(Hack.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(transactionService).createTransaction(any(), anyDouble(), anyString(), any());
        hackService.createNewHack(attacker, defender, 100.0);

        assertThat(attacker.getCredits()).isEqualTo(600.0);
        assertThat(defender.getCredits()).isEqualTo(200.0);

        verify(transactionService).createTransaction(attacker, 100.0, "Hack", TransactionType.RECEIVE);
        verify(transactionService).createTransaction(defender, 100.0, "Hack", TransactionType.SEND);
        verify(userRepository).save(attacker);
        verify(userRepository).save(defender);
        verify(hackRepository).save(argThat(hack -> hack.getStatus() == HackStatus.Succeeded));
    }

    @Test
    void shouldLimitHackToAvailableCredits_WhenDefenderHasLessThanRequested() {
        when(hackRepository.save(any(Hack.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(transactionService).createTransaction(any(), anyDouble(), anyString(), any());
        hackService.createNewHack(attacker, defender, 400.0); // More than defender's credits

        assertThat(attacker.getCredits()).isEqualTo(800.0);
        assertThat(defender.getCredits()).isEqualTo(0.0);

        verify(transactionService).createTransaction(attacker, 300.0, "Hack", TransactionType.RECEIVE);
        verify(transactionService).createTransaction(defender, 300.0, "Hack", TransactionType.SEND);
        verify(userRepository).save(attacker);
        verify(userRepository).save(defender);
        verify(hackRepository).save(argThat(hack -> hack.getStatus() == HackStatus.Succeeded));
    }

    @Test
    void shouldApplyOffenseUpgrade_WhenAttackerHasOne() {
        OffenseUpgrade offenseUpgrade = OffenseUpgrade.builder().owner(attacker).build();
        attacker.setOffenseUpgrade(offenseUpgrade);
        when(offenseUpgradeService.calculateCredits(100.0)).thenReturn(150.0); // Simulating upgrade bonus

        hackService.createNewHack(attacker, defender, 100.0);

        verify(offenseUpgradeService).calculateCredits(100.0);
        verify(transactionService).createTransaction(attacker, 150.0, "Hack", TransactionType.RECEIVE);
        verify(transactionService).createTransaction(defender, 150.0, "Hack", TransactionType.SEND);
        verify(userRepository).save(attacker);
        verify(userRepository).save(defender);
    }

    @Test
    void shouldChangeUserDefense() {
        User newDefender = User.builder().id(UUID.randomUUID()).username("newDefender").build();

        hackService.changeUserDefense(attacker, newDefender);

        assertThat(attacker.getDefense()).isEqualTo(newDefender);
    }

    @Test
    void shouldNotChangeDefense_WhenCurrentUserIsSameAsDefenseUser() {
        attacker.setDefense(null);

        hackService.changeUserDefense(attacker, attacker);

        assertThat(attacker.getDefense()).isNull();
    }

    @Test
    void shouldGetUserHackHistory() {
        List<Hack> hacks = List.of(Hack.builder().attacker(attacker).defender(defender).status(HackStatus.Succeeded).build());
        when(hackRepository.findByAttackerOrDefenderOrderByCreatedOnDesc(attacker, attacker)).thenReturn(hacks);

        List<Hack> result = hackService.getUserHistory(attacker);

        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(HackStatus.Succeeded);
        verify(hackRepository).findByAttackerOrDefenderOrderByCreatedOnDesc(attacker, attacker);
    }
}
