package app.hack.repository;

import app.hack.model.Hack;
import app.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HackRepository extends JpaRepository<Hack, UUID> {

}
