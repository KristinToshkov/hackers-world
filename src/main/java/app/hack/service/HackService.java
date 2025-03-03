package app.hack.service;

import app.defenseUpgrade.service.DefenseUpgradeService;
import app.hack.model.Hack;
import app.hack.model.HackStatus;
import app.hack.repository.HackRepository;
import app.offenseUpgrade.service.OffenseUpgradeService;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class HackService {

    private final HackRepository hackRepository;
    private final UserRepository userRepository;
    private final OffenseUpgradeService offenseUpgradeService;
    private final DefenseUpgradeService defenseUpgradeService;
    private final TransactionService transactionService;

    @Autowired
    public HackService(HackRepository hackRepository, UserRepository userRepository, OffenseUpgradeService offenseUpgradeService, DefenseUpgradeService defenseUpgradeService, TransactionService transactionService) {
        this.hackRepository = hackRepository;
        this.userRepository = userRepository;
        this.offenseUpgradeService = offenseUpgradeService;
        this.defenseUpgradeService = defenseUpgradeService;
        this.transactionService = transactionService;
    }

    @Transactional
    public void createNewHack(User attacker, User defender, Double credits) {
        Hack.HackBuilder hack = Hack.builder().attacker(attacker).defender(defender).createdOn(LocalDateTime.now());
        if(defender.getDefense() == attacker) {
            hack.status(HackStatus.Defended);
            hackRepository.save(hack.build());
        } else if (defender.getDefenseUpgrade() != null) {
            defenseUpgradeService.decreaseUses(defender.getDefenseUpgrade(), defender);
            hack.status(HackStatus.Defended);
            hackRepository.save(hack.build());
        }
        else {
            if(attacker.getOffenseUpgrade() != null)
                credits = offenseUpgradeService.calculateCredits(credits);
            if(credits > defender.getCredits()) {
                credits = defender.getCredits();
            }
                defender.setCredits(defender.getCredits() - credits);
                attacker.setCredits(attacker.getCredits() + credits);
                hack.credits(credits);

            transactionService.createTransaction(attacker,credits, "Hack", TransactionType.RECEIVE);
            transactionService.createTransaction(defender,credits, "Hack", TransactionType.SEND);
            hack.status(HackStatus.Succeeded);
            Hack build = hack.build();
            hackRepository.save(build);
            userRepository.save(attacker);
            userRepository.save(defender);
        }
    }

    @Transactional
    public void changeUserDefense(User currentUser, User defenseUser)
    {
        if(currentUser != defenseUser)
            currentUser.setDefense(defenseUser);
    }

    public List<Hack> getUserHistory(User user) {
        return hackRepository.findByAttackerOrDefenderOrderByCreatedOnDesc(user, user);

    }
}
