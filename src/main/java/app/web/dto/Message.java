package app.web.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Message {
    private LocalDateTime sentAt;
    private String message;
    private String author;
}
