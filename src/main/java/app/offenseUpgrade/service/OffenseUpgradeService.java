package app.offenseUpgrade.service;


import app.exception.DomainException;
import app.offenseUpgrade.model.OffenseUpgrade;
import app.offenseUpgrade.repository.OffenseUpgradeRepository;
import app.user.model.User;
import app.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.constant.Constants.OFFENSE_UPGRADE_MULTIPLER;
import static app.constant.Constants.OFFENSE_UPGRADE_PRICE;

@Service
@Slf4j
public class OffenseUpgradeService {

    private final UserRepository userRepository;
    private final OffenseUpgradeRepository offenseUpgradeRepository;

    public OffenseUpgradeService(UserRepository userRepository, OffenseUpgradeRepository offenseUpgradeRepository) {
        this.userRepository = userRepository;
        this.offenseUpgradeRepository = offenseUpgradeRepository;
    }

    public void createOffenseUpgrade(User user) {
        if(user.getOffenseUpgrade() != null) {
            throw new DomainException("Already owned!");
        }
        if(user.getCredits() < OFFENSE_UPGRADE_PRICE) {
            throw new DomainException("Not enough credits");
        }
        OffenseUpgrade offenseUpgrade = OffenseUpgrade.builder().owner(user).build();


        offenseUpgradeRepository.save(offenseUpgrade);
        user.setCredits(user.getCredits() - OFFENSE_UPGRADE_PRICE);
        user.setOffenseUpgrade(offenseUpgrade);
        userRepository.save(user);
    }

    public Double calculateCredits(Double credits) {
        return credits*OFFENSE_UPGRADE_MULTIPLER;
    }
}
