package app.defenseUpgrade.repository;

import app.defenseUpgrade.model.DefenseUpgrade;
import app.hack.model.Hack;
import app.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DefenseUpgradeRepository extends JpaRepository<DefenseUpgrade, UUID> {

}
