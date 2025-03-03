package app.defenseUpgrade.service;


import app.defenseUpgrade.model.DefenseUpgrade;
import app.defenseUpgrade.repository.DefenseUpgradeRepository;
import app.exception.DomainException;
import app.offenseUpgrade.repository.OffenseUpgradeRepository;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static app.constant.Constants.DEFENSE_UPGRADE_PRICE;
import static app.constant.Constants.OFFENSE_UPGRADE_PRICE;

@Slf4j
@Service
public class DefenseUpgradeService {

    private final DefenseUpgradeRepository defenseUpgradeRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Autowired
    public DefenseUpgradeService(DefenseUpgradeRepository defenseUpgradeRepository, UserRepository userRepository, TransactionService transactionService) {
        this.defenseUpgradeRepository = defenseUpgradeRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }


    @Transactional
    public void buyDefenseUpgrade(User user) {
        if(user.getCredits() < DEFENSE_UPGRADE_PRICE)
            throw new DomainException("You do not have enough credits");
        user.setCredits(user.getCredits() - DEFENSE_UPGRADE_PRICE);
        transactionService.createTransaction(user, DEFENSE_UPGRADE_PRICE, "Bought Defense Upgrade", TransactionType.SEND);
        if(user.getDefenseUpgrade() == null) {
            DefenseUpgrade newDefenseUpgrade = DefenseUpgrade.builder().owner(user).uses(1).build();
            defenseUpgradeRepository.save(newDefenseUpgrade);
            user.setDefenseUpgrade(newDefenseUpgrade);
        } else {
            user.getDefenseUpgrade().setUses(user.getDefenseUpgrade().getUses() + 1);
            defenseUpgradeRepository.save(user.getDefenseUpgrade());
        }
        userRepository.save(user);
    }

    public void decreaseUses(DefenseUpgrade defenseUpgrade, User defender) {
        defenseUpgrade.setUses(defenseUpgrade.getUses() - 1);
        if(defenseUpgrade.getUses() == 0) {
            defender.setDefenseUpgrade(null);
            defenseUpgradeRepository.delete(defenseUpgrade);
            userRepository.save(defender);
        }
    }
}
