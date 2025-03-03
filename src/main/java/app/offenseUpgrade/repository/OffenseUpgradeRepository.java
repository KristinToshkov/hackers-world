package app.offenseUpgrade.repository;


import app.offenseUpgrade.model.OffenseUpgrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OffenseUpgradeRepository extends JpaRepository<OffenseUpgrade, UUID> {
}
