package app.chatMessage.service;

import app.chatMessage.client.ChatMessageClient;
import app.web.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Slf4j
@Service
public class ChatMessageService {
    private final ChatMessageClient chatMessageClient;

    @Autowired
    public ChatMessageService(ChatMessageClient chatMessageClient) {
        this.chatMessageClient = chatMessageClient;
    }

    public void sendMessage(String author, String message) {
        Message msg = Message.builder().author(author).message(message).sentAt(LocalDateTime.now()).build();
        log.info("Sending message: " + msg);
        ResponseEntity<String> stringResponseEntity = chatMessageClient.sendMessage(msg);
    }

    public List<Message> getMessages() {
        ResponseEntity<List<Message>> response = chatMessageClient.getMessage();
        return response.getBody();
    }
}
