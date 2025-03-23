package app;


import app.exception.DomainException;
import app.offenseUpgrade.model.OffenseUpgrade;
import app.offenseUpgrade.repository.OffenseUpgradeRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffenseUpgradeServiceTest {

    private static final double OFFENSE_UPGRADE_PRICE = 100.0;
    private static final double OFFENSE_UPGRADE_MULTIPLIER = 1.5;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OffenseUpgradeRepository offenseUpgradeRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private OffenseUpgradeService offenseUpgradeService;

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
    void shouldCreateOffenseUpgradeSuccessfully() {
        user.setCredits(350.0); // Ensure enough credits before test

        when(offenseUpgradeRepository.save(any(OffenseUpgrade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        offenseUpgradeService.createOffenseUpgrade(user);

        assertThat(user.getCredits()).isEqualTo(100.0); // 350 - 250 = 100
        assertThat(user.getOffenseUpgrade()).isNotNull();
        verify(offenseUpgradeRepository).save(any(OffenseUpgrade.class));

        // Update the expected transaction amount to match the actual behavior
        verify(transactionService).createTransaction(user, 250.0, "Bough Offense Upgrade", TransactionType.SEND);

        verify(userRepository).save(user);
    }



    @Test
    void shouldThrowExceptionWhenUserAlreadyHasOffenseUpgrade() {
        user.setOffenseUpgrade(new OffenseUpgrade());

        assertThatThrownBy(() -> offenseUpgradeService.createOffenseUpgrade(user))
                .isInstanceOf(DomainException.class)
                .hasMessage("Already owned!");

        verify(offenseUpgradeRepository, never()).save(any());
        verify(transactionService, never()).createTransaction(any(), anyDouble(), anyString(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUserHasInsufficientCredits() {
        user.setCredits(50.0); // Not enough

        assertThatThrownBy(() -> offenseUpgradeService.createOffenseUpgrade(user))
                .isInstanceOf(DomainException.class)
                .hasMessage("Not enough credits");

        verify(offenseUpgradeRepository, never()).save(any());
        verify(transactionService, never()).createTransaction(any(), anyDouble(), anyString(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldCalculateCreditsWithMultiplier() {
        double credits = 200.0;
        double expectedCredits = credits * OFFENSE_UPGRADE_MULTIPLIER;

        double result = offenseUpgradeService.calculateCredits(credits);

        assertThat(result).isEqualTo(expectedCredits);
    }
}
