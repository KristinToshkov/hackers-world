package app.web;


import app.exception.DomainException;
import app.offenseUpgrade.service.OffenseUpgradeService;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class OffenseUpgradeController {

    private final UserService userService;
    private final OffenseUpgradeService offenseUpgradeService;

    @Autowired
    public OffenseUpgradeController(UserService userService, OffenseUpgradeService offenseUpgradeService) {
        this.userService = userService;
        this.offenseUpgradeService = offenseUpgradeService;
    }

    @GetMapping("/buy-offense-upgrade")
    public ModelAndView buyOffenseUpgrade(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        String username = authenticationMetadata.getUsername();
        User user = userService.getByUsername(username);
        try {
            offenseUpgradeService.createOffenseUpgrade(user);
        } catch (DomainException e) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("upgrades");
            modelAndView.addObject("errorOffenseUpgrade", e.getMessage());
            modelAndView.addObject("user", user);
            return modelAndView;
        }
        ModelAndView modelAndView = new ModelAndView("redirect:/home");
        modelAndView.addObject("user", user);
        return modelAndView;
    }
}
