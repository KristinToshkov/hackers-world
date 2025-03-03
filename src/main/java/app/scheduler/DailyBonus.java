package app.scheduler;

import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static app.constant.Constants.DAILY_BONUS;

@Component
@Slf4j
public class DailyBonus {
    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @Autowired
    public DailyBonus(TransactionService transactionService, UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedDelay = 300000) // Presumable a delay of 24 hours, however for the sake of the demo I have set the delay of only 5 minutes :)
    public void addCreditsToAllUsers() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            user.setCredits(user.getCredits() + 5);
            transactionService.createTransaction(user, DAILY_BONUS, "Daily Bonus", TransactionType.RECEIVE);
            log.info("Added Daily Bonus");
            userRepository.save(user);
        }
    }
}
