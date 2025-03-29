package app.API;

import app.security.AuthenticationMetadata;
import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.service.UserService;
import app.web.IndexController;
import app.web.dto.PasswordRequest;
import app.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TransactionService transactionService;

    // Helper method to create authenticated requests
    private Authentication createAuthentication(User user) {
        AuthenticationMetadata authMetadata = new AuthenticationMetadata(
                user.getId(),
                user.getUsername(),
                "password",
                user.getRole(),
                user.isActive()
        );
        return new UsernamePasswordAuthenticationToken(
                authMetadata,
                null,
                authMetadata.getAuthorities()
        );
    }

    // Helper method to create test user
    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setActive(true);
        user.setRole(UserRole.USER);
        return user;
    }

    @Test
    public void getIndexPage_ReturnsIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    public void getLoginPage_NoErrorParam_ReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("loginRequest"))
                .andExpect(model().attributeDoesNotExist("errorMessage"));
    }

    @Test
    public void getLoginPage_WithErrorParam_ShowsErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Incorrect username or password!"));
    }

    @Test
    public void getRegisterPage_ReturnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    public void getDarknetPage_Authenticated_ReturnsDarknetViewWithUser() throws Exception {
        User user = createTestUser();
        when(userService.getById(user.getId())).thenReturn(user);

        mockMvc.perform(get("/darknet")
                        .with(authentication(createAuthentication(user))))
                .andExpect(status().isOk())
                .andExpect(view().name("darknet"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    public void getHomePage_Authenticated_ReturnsHomeViewWithUserAndMessage() throws Exception {
        User user = createTestUser();
        when(userService.getById(user.getId())).thenReturn(user);

        mockMvc.perform(get("/home")
                        .with(authentication(createAuthentication(user))))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    public void getHackPage_Authenticated_ReturnsHackViewWithUsers() throws Exception {
        User user = createTestUser();
        List<User> otherUsers = List.of(createTestUser(), createTestUser());

        when(userService.getById(user.getId())).thenReturn(user);
        when(userService.getAllUsersExceptMe(user.getUsername())).thenReturn(otherUsers);

        mockMvc.perform(get("/hack-on")
                        .with(authentication(createAuthentication(user))))
                .andExpect(status().isOk())
                .andExpect(view().name("hack-on"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("allUsersExceptMe", otherUsers));
    }
    @Test
    public void getDashboard_NonAdminUser_RedirectsToHome() throws Exception {
        User regularUser = createTestUser();
        when(userService.getById(regularUser.getId())).thenReturn(regularUser);
        when(userService.isAdmin(regularUser)).thenReturn(false);

        mockMvc.perform(get("/dashboard")
                        .with(authentication(createAuthentication(regularUser))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    public void postForgotPassword_ValidRequest_ShowsSuccess() throws Exception {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("testUser");

        mockMvc.perform(post("/forgot-password")
                        .param("username", request.getUsername()))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("successMessage"));
    }

    @Test
    public void postForgotPassword_InvalidRequest_ShowsError() throws Exception {
        mockMvc.perform(post("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"));
    }

    @Test
    public void postRegister_ValidRequest_RedirectsToLogin() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setPassword("password");
        request.setConfirmPassword("password");
        request.setEmail("new@example.com");

        mockMvc.perform(post("/register")
                        .flashAttr("registerRequest", request))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void postRegister_InvalidRequest_ReturnsLoginView() throws Exception {
        RegisterRequest request = new RegisterRequest(); // Invalid empty request

        mockMvc.perform(post("/register")
                        .flashAttr("registerRequest", request))
                .andExpect(status().isFound())
                .andExpect(view().name("redirect:/login"));
    }
}