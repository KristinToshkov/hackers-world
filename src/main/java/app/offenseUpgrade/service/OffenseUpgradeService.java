package app.offenseUpgrade.service;


import app.exception.DomainException;
import app.offenseUpgrade.model.OffenseUpgrade;
import app.offenseUpgrade.repository.OffenseUpgradeRepository;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static app.constant.Constants.OFFENSE_UPGRADE_MULTIPLER;
import static app.constant.Constants.OFFENSE_UPGRADE_PRICE;

@Service
@Slf4j
public class OffenseUpgradeService {

    private final UserRepository userRepository;
    private final OffenseUpgradeRepository offenseUpgradeRepository;
    private final TransactionService transactionService;

    @Autowired
    public OffenseUpgradeService(UserRepository userRepository, OffenseUpgradeRepository offenseUpgradeRepository, TransactionService transactionService) {
        this.userRepository = userRepository;
        this.offenseUpgradeRepository = offenseUpgradeRepository;
        this.transactionService = transactionService;
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
        transactionService.createTransaction(user, OFFENSE_UPGRADE_PRICE, "Bough Offense Upgrade", TransactionType.SEND);
        user.setOffenseUpgrade(offenseUpgrade);
        userRepository.save(user);
    }

    public Double calculateCredits(Double credits) {
        return credits*OFFENSE_UPGRADE_MULTIPLER;
    }
}
