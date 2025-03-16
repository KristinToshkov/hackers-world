package app.chatMessage.client;


import app.web.dto.Message;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "chat-service", url = "http://localhost:8081/api/chat")
public interface ChatMessageClient {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> sendMessage(@RequestBody Message message);

    @GetMapping
    ResponseEntity<List<Message>> getMessage();
}
