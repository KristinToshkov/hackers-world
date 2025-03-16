package app.web;

import app.chatMessage.service.ChatMessageService;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/darknet")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final UserService userService;

    public ChatMessageController(ChatMessageService chatMessageService, UserService userService) {
        this.chatMessageService = chatMessageService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Message>> getChatMessages() {
        List<Message> messages = chatMessageService.getMessages();
        log.info("Getting chat messages");
        return ResponseEntity.ok(messages);
    }


    @PostMapping
    public ResponseEntity<String> sendMessage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata, @RequestParam String message) {
        log.info("Sending chat message");
        User user = userService.getByUsername(authenticationMetadata.getUsername());
        chatMessageService.sendMessage(user.getUsername(), message);
        return ResponseEntity.ok("Message sent successfully");
    }
}

