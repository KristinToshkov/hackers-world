package app.user.model;

import app.defenseUpgrade.model.DefenseUpgrade;
import app.offenseUpgrade.model.OffenseUpgrade;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "players")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    private String profilePicture;


    private String email;

    @Column(nullable = false)
    private String password;

    private Integer userRank;

    private Double credits;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = true, unique = false)
    private User defense;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private boolean isActive;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @OneToOne
    private DefenseUpgrade defenseUpgrade;

    @OneToOne
    private OffenseUpgrade offenseUpgrade;

}
