package app.IT;

import app.exception.DomainException;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.web.dto.PasswordRequest;
import app.web.dto.RegisterRequest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")  // Ensure we use the test profile
@Transactional  // Rollback changes after each test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)  // Reset context between tests
class UserServiceIT {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        Cache cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.clear();
        }
    }


    @Test
    void testRegisterUser_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setConfirmPassword("password");

        // When
        User registeredUser = userService.register(request);

        // Then
        assertNotNull(registeredUser);
        assertEquals("testuser", registeredUser.getUsername());
        assertTrue(passwordEncoder.matches("password", registeredUser.getPassword()));
        assertEquals(UserRole.USER, registeredUser.getRole());
        assertTrue(registeredUser.isActive());
    }

    @Test
    void testRegisterUser_FailsWhenUsernameExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existingUser");
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setConfirmPassword("password");

        // Save an existing user
        userRepository.save(User.builder()
                .username("existingUser")
                .email("existing@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> userService.register(request));
        assertEquals("Username \"existingUser\" unavailable.", exception.getMessage());
    }

    @Test
    void testRegisterUser_FailsWhenPasswordsDoNotMatch() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password1");
        request.setConfirmPassword("password2");

        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> userService.register(request));
        assertEquals("Passwords do not match.", exception.getMessage());
    }

    @Test
    void testGetAllUsers_ShouldReturnOnlyActiveUsers() {
        // Given
        userRepository.save(User.builder()
                .username("activeUser")
                .email("active@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .userRank(0)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        userRepository.save(User.builder()
                .username("inactiveUser")
                .email("inactive@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .userRank(0)
                .isActive(false) // Inactive
                .createdOn(LocalDateTime.now())
                .build());

        // When
        List<User> users = userService.getAllUsers();
        users.forEach(u -> System.out.println(u.getUsername()));
        // Then
        assertEquals(2, users.size()); // We expect 2 because we always have KrisRoot as a default user init
        assertEquals("activeUser", users.get(1).getUsername());
    }

    @Test
    void testSwitchStatus_TogglesUserActiveStatus() {
        // Given
        User user = userRepository.save(User.builder()
                .username("testUser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        // When
        userService.switchStatus(user.getId());
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertFalse(updatedUser.isActive());

        // When (toggle again)
        userService.switchStatus(user.getId());
        updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertTrue(updatedUser.isActive());
    }

    @Test
    void testResetPassword_SetsPasswordToDefault() {
        // Given
        User user = userRepository.save(User.builder()
                .username("testUser")
                .email("test@example.com")
                .password(passwordEncoder.encode("oldpassword"))
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        PasswordRequest passwordRequest = new PasswordRequest();
        passwordRequest.setUsername("testUser");

        // When
        userService.resetPassword(passwordRequest);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertTrue(passwordEncoder.matches("000000", updatedUser.getPassword()));
    }

    @Test
    void testPromoteUser_ChangesRoleToAdmin() {
        // Given
        User user = userRepository.save(User.builder()
                .username("testUser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        // When
        userService.promoteUser(user);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertEquals(UserRole.ADMIN, updatedUser.getRole());
    }

    @Test
    void testBanUser_SetsUserToInactive() {
        // Given
        User user = userRepository.save(User.builder()
                .username("testUser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        // When
        userService.banUser(user);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertFalse(updatedUser.isActive());
    }

    @Test
    void testRankUpUser_FailsWhenNotEnoughCredits() {
        // Given
        User user = userRepository.save(User.builder()
                .username("testUser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .isActive(true)
                .credits(30.0) // Not enough credits
                .createdOn(LocalDateTime.now())
                .build());

        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> userService.rankUpUser(user));
        assertEquals("You need 50 credits to rank up!", exception.getMessage());
    }

    @Test
    void testRankUpUser_SuccessWhenEnoughCredits() {
        // Given
        User user = userRepository.save(User.builder()
                .username("testUser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.USER)
                .isActive(true)
                .userRank(0)
                .credits(60.0) // Enough credits
                .createdOn(LocalDateTime.now())
                .build());

        // When
        userService.rankUpUser(user);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertEquals(10.0, updatedUser.getCredits()); // 60 - 50 = 10
        assertEquals(1, updatedUser.getUserRank());
    }
}
