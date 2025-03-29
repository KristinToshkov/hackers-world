package app.API;

import app.chatMessage.service.ChatMessageService;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.service.UserService;
import app.web.ChatMessageController;
import app.web.dto.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ChatMessageController.class)
public class ChatMessageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatMessageService chatMessageService;

    @MockBean
    private UserService userService;

    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .isActive(true)
                .role(UserRole.USER)
                .build();
    }

    private Message createTestMessage(String author, String content) {
        return Message.builder()
                .author(author)
                .message(content)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @BeforeEach
    void setupAuthentication() {
        User user = createTestUser();
        AuthenticationMetadata authMetadata = AuthenticationMetadata.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password("password")
                .role(user.getRole())
                .isActive(user.isActive())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        authMetadata,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @Test
    public void getMessages_ReturnsListOfMessages() throws Exception {
        // Arrange
        Message message1 = createTestMessage("user1", "Hello");
        Message message2 = createTestMessage("user2", "Hi there");
        List<Message> mockMessages = List.of(message1, message2);

        when(chatMessageService.getMessages()).thenReturn(mockMessages);

        // Act & Assert
        mockMvc.perform(get("/api/darknet")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].author", is("user1")))
                .andExpect(jsonPath("$[0].message", is("Hello")))
                .andExpect(jsonPath("$[0].sentAt", notNullValue()))
                .andExpect(jsonPath("$[1].author", is("user2")))
                .andExpect(jsonPath("$[1].message", is("Hi there")))
                .andExpect(jsonPath("$[1].sentAt", notNullValue()));
    }

    @Test
    public void sendMessage_ValidRequest_ReturnsSuccess() throws Exception {
        // Arrange
        User user = createTestUser();
        String testMessage = "Test message";

        when(userService.getByUsername("testUser")).thenReturn(user);

        // Act & Assert
        mockMvc.perform(post("/api/darknet")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("message", testMessage))
                .andExpect(status().isOk())
                .andExpect(content().string("Message sent successfully"));

        verify(chatMessageService).sendMessage(user.getUsername(), testMessage);
    }

    @Test
    public void sendMessage_EmptyMessage_ReturnsServerError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/darknet")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("message", ""))
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void sendMessage_Unauthenticated_Redirects() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        mockMvc.perform(post("/api/darknet")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("message", "test"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void sendMessage_UserNotFound_ReturnsServerError() throws Exception {
        // Arrange
        when(userService.getByUsername("testUser")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/darknet")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("message", "test"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void sendMessage_ServiceThrowsException_ReturnsServerError() throws Exception {
        // Arrange
        User user = createTestUser();
        when(userService.getByUsername("testUser")).thenReturn(user);
        doThrow(new RuntimeException("Service failure"))
                .when(chatMessageService).sendMessage(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/darknet")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("message", "test"))
                .andExpect(status().isInternalServerError());
    }
}