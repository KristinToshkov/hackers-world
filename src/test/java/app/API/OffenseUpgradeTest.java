package app.API;

import app.exception.DomainException;
import app.offenseUpgrade.service.OffenseUpgradeService;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.service.UserService;
import app.web.OffenseUpgradeController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OffenseUpgradeController.class)
public class OffenseUpgradeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private OffenseUpgradeService offenseUpgradeService;

    @Test
    public void buyOffenseUpgrade_SuccessfulPurchase_RedirectsToHome() throws Exception {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setPassword("testPassword");
        user.setActive(true);
        user.setRole(UserRole.USER);
        user.setCredits(100.00);
        user.setCreatedOn(LocalDateTime.now());

        // Create properly constructed AuthenticationMetadata
        AuthenticationMetadata authMetadata = new AuthenticationMetadata(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );

        // Create Authentication object
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authMetadata,
                null,
                authMetadata.getAuthorities()
        );

        // Mock services
        when(userService.getByUsername("testUser")).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/buy-offense-upgrade")
                        .with(authentication(authentication)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
        verify(offenseUpgradeService).createOffenseUpgrade(user);
    }

    @Test
    public void buyOffenseUpgrade_DomainException_ReturnsUpgradesViewWithError() throws Exception {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setPassword("testPassword");
        user.setActive(true);
        user.setRole(UserRole.USER);
        user.setCredits(100.00);
        user.setCreatedOn(LocalDateTime.now());
        String errorMessage = "You do not have enough credits";

        // Create AuthenticationMetadata instance
        AuthenticationMetadata authMetadata = new AuthenticationMetadata(
                user.getId(),       // userId
                user.getUsername(), // username
                user.getPassword(), // password
                user.getRole(),     // role
                user.isActive()     // isActive
        );
        // Create Authentication object
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authMetadata,
                null,
                authMetadata.getAuthorities()
        );

        // Mock services - KEY FIX HERE
        when(userService.getByUsername("testUser")).thenReturn(user);
        doThrow(new DomainException(errorMessage))
                .when(offenseUpgradeService).createOffenseUpgrade(user);  // Correct syntax for void methods

        // Act & Assert
        mockMvc.perform(get("/buy-offense-upgrade")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(view().name("upgrades"))
                .andExpect(model().attributeExists("errorOffenseUpgrade"))
                .andExpect(model().attribute("errorOffenseUpgrade", errorMessage))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    public void buyOffenseUpgrade_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/buy-offense-upgrade"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    public void buyOffenseUpgrade_UserNotFound_ReturnsHome() throws Exception {
        // Arrange
        User user = new User();
        user.setUsername("testUser");
        user.setActive(true);
        user.setRole(UserRole.USER);

        AuthenticationMetadata authMetadata = new AuthenticationMetadata(
                UUID.randomUUID(),
                "testUser",
                "password",
                UserRole.USER,
                true
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authMetadata,
                null,
                authMetadata.getAuthorities()
        );

        when(userService.getByUsername("testUser")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/buy-offense-upgrade")
                        .with(authentication(authentication)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    public void buyOffenseUpgrade_ServiceThrowsRuntimeException_ReturnsErrorPage() throws Exception {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setActive(true);
        user.setRole(UserRole.USER);

        AuthenticationMetadata authMetadata = new AuthenticationMetadata(
                user.getId(),
                user.getUsername(),
                "password",
                user.getRole(),
                user.isActive()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authMetadata,
                null,
                authMetadata.getAuthorities()
        );

        when(userService.getByUsername("testUser")).thenReturn(user);
        doThrow(new RuntimeException("Service failure"))
                .when(offenseUpgradeService).createOffenseUpgrade(user);

        // Act & Assert
        mockMvc.perform(get("/buy-offense-upgrade")
                        .with(authentication(authentication)))
                .andExpect(status().is5xxServerError())
                .andExpect(view().name("error"));
    }
}