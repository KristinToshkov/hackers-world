package app.offenseUpgrade.model;


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
public class OffenseUpgrade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Double multiplier;

    @OneToOne
    private User owner;

    @Column(nullable = false)
    private Double price;
}
