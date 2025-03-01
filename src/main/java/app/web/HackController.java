package app.web;

import app.exception.DomainException;
import app.hack.service.HackService;
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
    public ModelAndView attackUser(@PathVariable UUID id, @Valid HackRequest userHackRequest, BindingResult bindingResult, @AuthenticationPrincipal UserDetails userDetails) {

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
        User attacker = userService.getByUsername(userDetails.getUsername());
        hackService.createNewHack(attacker, defender, userHackRequest.getCredits());

        return new ModelAndView("redirect:/hack-on");
    }

    @GetMapping("/defend/{id}")
    public ModelAndView defendUser(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        String usernameOfSession = userDetails.getUsername();
        User userOfSession = userService.getByUsername(usernameOfSession);
        User userToDefend = userService.getById(id);
        hackService.changeUserDefense(userOfSession, userToDefend);
        return new ModelAndView("redirect:/hack-on");
    }
}
