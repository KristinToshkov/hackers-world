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
    public void createNewHack(User attacker, User defender, Integer credits) {
        Hack.HackBuilder hack = Hack.builder().attacker(attacker).defender(defender);
        if(defender.getDefense() == attacker) {
            hack.status(HackStatus.Defended);
            hackRepository.save(hack.build());
        } else {
            if(credits > defender.getCredits()) {
                defender.setCredits(0);
                attacker.setCredits(attacker.getCredits() + defender.getCredits());
            } else {
                defender.setCredits(defender.getCredits() - credits);
                attacker.setCredits(attacker.getCredits() + credits);
            }
            hack.credits(credits);
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
}
