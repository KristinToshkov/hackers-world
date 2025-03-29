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
// url changed to deployed microservice - previously was https://localhost:8081/api/chat
@FeignClient(name = "chat-service", url = "https://chat-microservice-f0be7c121a26.herokuapp.com/api/chat")
public interface ChatMessageClient {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> sendMessage(@RequestBody Message message);

    @GetMapping
    ResponseEntity<List<Message>> getMessage();
}
