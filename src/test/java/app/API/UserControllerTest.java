package app.API;

import app.hack.service.HackService;
import app.security.AuthenticationMetadata;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.web.UserController;
import app.web.dto.UserEditRequest;
import com.sun.security.auth.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private HackService hackService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserRepository userRepository;

    @WithMockUser(username = "testUser", roles = {"USER"})
    @Test
    void testGetProfileMenu_ShouldReturnProfileView() throws Exception {
        // Mock user data
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setEmail("test@example.com");
        user.setRole(UserRole.USER);
        user.setActive(true);
        user.setCreatedOn(LocalDateTime.now());

        // Mock authentication metadata
        AuthenticationMetadata authMetadata = new AuthenticationMetadata(userId, "testUser", "123123asd", UserRole.USER, true);

        // Mock service call
        when(userService.getById(userId)).thenReturn(user);

        // Perform request
        mockMvc.perform(get("/users/profile")
                        .with(user(authMetadata)) // Custom security principal if needed
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", user));
    }

    @WithMockUser(username = "testUser", roles = {"USER"})
    @Test
    void testUpdateUserProfile_ShouldRedirectOnSuccess() throws Exception {
        // Mock authentication metadata
        UUID userId = UUID.randomUUID();
        AuthenticationMetadata authMetadata = new AuthenticationMetadata(userId, "testUser", "123123asd", UserRole.USER, true);

        // Mock valid request
        UserEditRequest editRequest = new UserEditRequest();
        editRequest.setUsername("newUsername");
        editRequest.setEmail("new@example.com");

        doNothing().when(userService).editUserDetails(any(UUID.class), any(UserEditRequest.class));

        // Perform request
        mockMvc.perform(put("/users/profile")
                        .with(user(authMetadata))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "newUsername")
                        .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    @Test
    void testBanUser_AsAdmin_ShouldRedirect() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID userIdToBan = UUID.randomUUID();

        User adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setUsername("adminUser");
        adminUser.setRole(UserRole.ADMIN);

        User userToBan = new User();
        userToBan.setId(userIdToBan);
        userToBan.setUsername("targetUser");

        when(userService.getById(adminId)).thenReturn(adminUser);
        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(userService.getById(userIdToBan)).thenReturn(userToBan);
        doNothing().when(userService).banUser(userToBan);

        mockMvc.perform(get("/users/ban/" + userIdToBan)
                        .with(user(new AuthenticationMetadata(adminId, "adminUser", "123123asd", UserRole.USER, true))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @WithMockUser(username = "testUser", roles = {"USER"})
    @Test
    void testBanUser_AsUser_ShouldRedirect() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setUsername("testUser");
        normalUser.setRole(UserRole.USER);

        when(userService.getById(userId)).thenReturn(normalUser);
        when(userService.isAdmin(normalUser)).thenReturn(false);

        mockMvc.perform(get("/users/ban/" + targetUserId)
                        .with(user(new AuthenticationMetadata(userId, "testUser", "123123asd", UserRole.USER, true))))
                .andExpect(status().isFound());
    }

    @WithMockUser(username = "testUser", roles = {"USER"})
    @Test
    void testGetProfileMenu_Unauthenticated_ShouldReturnError() throws Exception {
        SecurityContextHolder.clearContext(); // Remove authentication

        mockMvc.perform(get("/users/profile"))
                .andExpect(status().is5xxServerError());
    }
}
