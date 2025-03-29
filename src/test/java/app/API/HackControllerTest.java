package app.API;

import app.exception.DomainException;
import app.hack.model.Hack;
import app.hack.service.HackService;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.service.UserService;
import app.web.HackController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HackController.class)
@WithMockUser(username = "testUser") // Default mock user for all tests
public class HackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HackService hackService;

    @MockBean
    private UserService userService;

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
    public void hackUser_ReturnsAttackViewWithUserAndForm() throws Exception {
        UUID userId = UUID.randomUUID();
        User targetUser = createTestUser();
        targetUser.setId(userId);

        when(userService.getById(userId)).thenReturn(targetUser);

        mockMvc.perform(get("/hack/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(view().name("attack"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("userHackRequest"));
    }

    @Test
    public void attackUser_ValidRequest_RedirectsToHackOn() throws Exception {
        UUID targetId = UUID.randomUUID();
        User attacker = createTestUser();
        User defender = createTestUser();
        defender.setId(targetId);

        when(userService.getByUsername("testUser")).thenReturn(attacker);
        when(userService.getById(targetId)).thenReturn(defender);

        mockMvc.perform(post("/hack/{id}", targetId)
                        .param("credits", "10.0")) // Instead of flash attributes
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hack-on"));

        verify(hackService).createNewHack(attacker, defender, 10.0);
    }

    @Test
    public void attackUser_InvalidRequest_Redirects() throws Exception {
        UUID targetId = UUID.randomUUID();
        User user = createTestUser();
        user.setId(targetId);

        when(userService.getById(targetId)).thenReturn(user);

        mockMvc.perform(post("/hack/{id}", targetId)) // No credits parameter = invalid
                .andExpect(status().isFound())
                .andExpect(view().name("redirect:/hack-on"));
    }

    @Test
    public void defendUser_RedirectsToHackOn() throws Exception {
        UUID targetId = UUID.randomUUID();
        User currentUser = createTestUser();
        User targetUser = createTestUser();
        targetUser.setId(targetId);

        when(userService.getByUsername("testUser")).thenReturn(currentUser);
        when(userService.getById(targetId)).thenReturn(targetUser);

        mockMvc.perform(get("/defend/{id}", targetId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hack-on"));

        verify(hackService).changeUserDefense(currentUser, targetUser);
    }

    @Test
    public void getCreditsPage_ReturnsCreditsViewWithUser() throws Exception {
        User user = createTestUser();
        when(userService.getByUsername("testUser")).thenReturn(user);

        mockMvc.perform(get("/credits"))
                .andExpect(status().isOk())
                .andExpect(view().name("credits"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    public void rankUp_Success_ReturnsCreditsView() throws Exception {
        User user = createTestUser();
        when(userService.getByUsername("testUser")).thenReturn(user);

        mockMvc.perform(get("/rank-up"))
                .andExpect(status().isOk())
                .andExpect(view().name("credits"))
                .andExpect(model().attributeExists("user"));

        verify(userService).rankUpUser(user);
    }

    @Test
    public void rankUp_Failure_ReturnsCreditsViewWithError() throws Exception {
        User user = createTestUser();
        String errorMessage = "Not enough credits";

        when(userService.getByUsername("testUser")).thenReturn(user);
        doThrow(new DomainException(errorMessage)).when(userService).rankUpUser(user);

        mockMvc.perform(get("/rank-up"))
                .andExpect(status().isOk())
                .andExpect(view().name("credits"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", errorMessage));
    }
}