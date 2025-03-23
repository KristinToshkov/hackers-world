package app;

import app.exception.DomainException;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("encodedpassword")
                .role(UserRole.USER)
                .isActive(true)
                .credits(100.0)
                .userRank(1)
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("new@example.com", "newuser", "password123", "password123");

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registeredUser = userService.register(request);

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getUsername()).isEqualTo(request.getUsername());
        assertThat(registeredUser.getPassword()).isEqualTo("encodedpassword");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldNotRegisterUserWhenUsernameIsTaken() {
        RegisterRequest request = new RegisterRequest("test@example.com", "testuser", "password", "password");

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Username \"testuser\" unavailable.");
    }

    @Test
    void shouldNotRegisterUserWhenPasswordsDoNotMatch() {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123", "password456");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Passwords do not match.");
    }

    @Test
    void shouldDecreaseUserCredits() {
        userService.decreaseCredits(user, 20);
        assertThat(user.getCredits()).isEqualTo(80.0);
    }

    @Test
    void shouldIncreaseUserCredits() {
        userService.increaseCredits(user, 50);
        assertThat(user.getCredits()).isEqualTo(150.0);
    }

    @Test
    void shouldGetUserById() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User foundUser = userService.getById(userId);

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(userId);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundById() {
        UUID fakeId = UUID.randomUUID();
        when(userRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(fakeId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("User with id");
    }

    @Test
    void shouldSwitchUserStatus() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.switchStatus(userId);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void shouldSwitchUserRole() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.switchRole(userId);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void shouldBanUser() {
        userService.banUser(user);
        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void shouldUnbanUser() {
        user.setActive(false);
        userService.unbanUser(user);
        assertThat(user.isActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void shouldPromoteUserToAdmin() {
        userService.promoteUser(user);
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void shouldDemoteUserToRegularUser() {
        user.setRole(UserRole.ADMIN);
        userService.demoteUser(user);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenRankingUpWithoutEnoughCredits() {
        user.setCredits(40.0);

        assertThatThrownBy(() -> userService.rankUpUser(user))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("You need 50 credits to rank up!");
    }

    @Test
    void shouldRankUpUserWhenEnoughCredits() {
        user.setCredits(50.0);

        userService.rankUpUser(user);

        assertThat(user.getCredits()).isEqualTo(0);
        assertThat(user.getUserRank()).isEqualTo(2);
        verify(userRepository).save(user);
    }
}
