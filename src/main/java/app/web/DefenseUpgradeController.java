package app.web;

import app.defenseUpgrade.service.DefenseUpgradeService;
import app.exception.DomainException;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DefenseUpgradeController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final DefenseUpgradeService defenseUpgradeService;

    @Autowired
    public DefenseUpgradeController(UserRepository userRepository, UserService userService, DefenseUpgradeService defenseUpgradeService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.defenseUpgradeService = defenseUpgradeService;
    }

    @GetMapping("/buy-defense-upgrade")
    public ModelAndView buyDefenseUpgrade(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {
        User user = userService.getByUsername(authenticationMetadata.getUsername());
        ModelAndView modelAndView = new ModelAndView("upgrades");
        modelAndView.addObject("user", user);
        try {
            defenseUpgradeService.buyDefenseUpgrade(user);
        } catch (DomainException e) {
            modelAndView.addObject("errorDefenseUpgrade", e.getMessage());
        }
        return modelAndView;
    }
}
