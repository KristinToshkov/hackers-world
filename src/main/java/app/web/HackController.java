package app.web;

import app.exception.DomainException;
import app.hack.model.Hack;
import app.hack.model.HackStatus;
import app.hack.service.HackService;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.service.UserService;
import app.web.Mapper.DtoMapper;
import app.web.dto.HackRequest;
import app.web.dto.UserEditRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
public class HackController {
    private final HackService hackService;
    private final UserService userService;

    @Autowired
    public HackController(HackService hackService, UserService userService) {
        this.hackService = hackService;
        this.userService = userService;
    }

    @GetMapping("/hack/{id}")
    public ModelAndView hackUser(@PathVariable UUID id) {
        User user = userService.getById(id);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("attack");
        modelAndView.addObject("user", user);
        modelAndView.addObject("userHackRequest", new HackRequest());
        return modelAndView;
    }

    @PostMapping("/hack/{id}")
    public ModelAndView attackUser(@PathVariable UUID id, @Valid HackRequest userHackRequest, BindingResult bindingResult, @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        if (bindingResult.hasErrors()) {
            log.info("Hack request validation error for " + userHackRequest.getCredits() + " credits");
            User user = userService.getById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("attack");
            modelAndView.addObject("user", user);
            modelAndView.addObject("userHackRequest", userHackRequest);
            modelAndView.addObject("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return modelAndView;
        }

            User defender = userService.getById(id);
        User attacker = userService.getByUsername(authenticationMetadata.getUsername());
        hackService.createNewHack(attacker, defender, userHackRequest.getCredits());

        return new ModelAndView("redirect:/hack-on");
    }

    @GetMapping("/defend/{id}")
    public ModelAndView defendUser(@PathVariable UUID id, @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {
        String usernameOfSession = authenticationMetadata.getUsername();
        User userOfSession = userService.getByUsername(usernameOfSession);
        User userToDefend = userService.getById(id);
        hackService.changeUserDefense(userOfSession, userToDefend);
        return new ModelAndView("redirect:/hack-on");
    }

    @GetMapping("/credits")
    public ModelAndView getCreditsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        String username = authenticationMetadata.getUsername();
        User user = userService.getByUsername(username);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("credits");
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @GetMapping("/rank-up")
    public ModelAndView rankUp(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        String username = authenticationMetadata.getUsername();
        User user = userService.getByUsername(username);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("credits");
        modelAndView.addObject("user", user);
        try {
            userService.rankUpUser(user);
        } catch (DomainException e) {
            modelAndView.addObject("error", e.getMessage());
        }
        return modelAndView;
    }

    @GetMapping("/history")
    public ModelAndView getHistory(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        String username = authenticationMetadata.getUsername();
        User user = userService.getByUsername(username);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("history");
        modelAndView.addObject("user", user);
        List<Hack> hacks = hackService.getUserHistory(user);
        modelAndView.addObject("hacks", hacks);
        modelAndView.addObject("hackStatusDefended", HackStatus.Defended);
        return modelAndView;
    }
}
