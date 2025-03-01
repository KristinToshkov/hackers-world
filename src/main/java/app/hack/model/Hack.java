package app.hack.model;

import app.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Hack {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private User attacker;

    @ManyToOne
    private User defender;

    private Integer credits;

    @Enumerated(EnumType.STRING)
    private HackStatus status;
}
