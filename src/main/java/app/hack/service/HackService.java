package app.hack.service;

import app.hack.model.Hack;
import app.hack.model.HackStatus;
import app.hack.repository.HackRepository;
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

    @Autowired
    public HackService(HackRepository hackRepository, UserRepository userRepository) {
        this.hackRepository = hackRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNewHack(User attacker, User defender, Double credits) {
        Hack.HackBuilder hack = Hack.builder().attacker(attacker).defender(defender).createdOn(LocalDateTime.now());
        if(defender.getDefense() == attacker) {
            hack.status(HackStatus.Defended);
            hackRepository.save(hack.build());
        } else {
            if(credits > defender.getCredits()) {
                hack.credits(defender.getCredits());
                attacker.setCredits(attacker.getCredits() + defender.getCredits());
                defender.setCredits((double) 0);
            } else {
                defender.setCredits(defender.getCredits() - credits);
                attacker.setCredits(attacker.getCredits() + credits);
                hack.credits(credits);
            }
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
