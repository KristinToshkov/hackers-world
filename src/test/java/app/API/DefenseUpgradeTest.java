package app.API;

import app.defenseUpgrade.service.DefenseUpgradeService;
import app.exception.DomainException;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.web.DefenseUpgradeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DefenseUpgradeController.class)
public class DefenseUpgradeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private DefenseUpgradeService defenseUpgradeService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setActive(true);
        user.setRole(UserRole.USER);
        user.setCredits(100.0);
        return user;
    }

    private AuthenticationMetadata createAuthMetadata(User user) {
        return new AuthenticationMetadata(
                user.getId(),
                user.getUsername(),
                "password",
                user.getRole(),
                user.isActive()
        );
    }

    @BeforeEach
    void setupAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        createAuthMetadata(createTestUser()),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @Test
    public void buyDefenseUpgrade_SuccessfulPurchase_ReturnsUpgradesView() throws Exception {
        // Arrange
        User user = createTestUser();
        when(userService.getByUsername("testUser")).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/buy-defense-upgrade"))
                .andExpect(status().isOk())
                .andExpect(view().name("upgrades"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeDoesNotExist("errorDefenseUpgrade"));

        verify(defenseUpgradeService).buyDefenseUpgrade(user);
    }

    @Test
    public void buyDefenseUpgrade_Failure_ReturnsUpgradesViewWithError() throws Exception {
        // Arrange
        User user = createTestUser();
        String errorMessage = "Not enough credits";

        when(userService.getByUsername("testUser")).thenReturn(user);
        doThrow(new DomainException(errorMessage))
                .when(defenseUpgradeService).buyDefenseUpgrade(user);

        // Act & Assert
        mockMvc.perform(get("/buy-defense-upgrade"))
                .andExpect(status().isOk())
                .andExpect(view().name("upgrades"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("errorDefenseUpgrade"))
                .andExpect(model().attribute("errorDefenseUpgrade", errorMessage));
    }
}