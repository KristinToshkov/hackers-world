package app.web;

import app.message.WelcomeMessage;
import app.exception.DomainException;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.LoginRequest;
import app.web.dto.PasswordRequest;
import app.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Random;


@Slf4j
@Controller
public class IndexController {

    private final UserService userService;

    @Autowired
    public IndexController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String getIndexPage() {

        return "index";
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(value = "error", required = false) String errorParam) {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("loginRequest", new LoginRequest());

        if (errorParam != null) {
            modelAndView.addObject("errorMessage", "Incorrect username or password!");
        }

        return modelAndView;
    }


    @GetMapping("/forgot-password")
    public ModelAndView getForgotPassword(@RequestParam(value = "error", required = false) String errorParam) {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("forgot-password");
        modelAndView.addObject("passwordRequest", new PasswordRequest());

        if (errorParam != null) {
            modelAndView.addObject("errorMessage", "Incorrect username!");
        }

        return modelAndView;
    }

    @PostMapping("/forgot-password")
    public ModelAndView getPassword(@Valid PasswordRequest passwordRequest, BindingResult bindingResult) {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("forgot-password");

        if (bindingResult.hasErrors()) {
            modelAndView.addObject("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return modelAndView;
        }
        try {
            userService.resetPassword(passwordRequest);
            modelAndView.addObject("successMessage", "Password has been reset!");
            return modelAndView;
        } catch (DomainException e) {
                modelAndView.addObject("usernameError", e.getMessage());

            return modelAndView;
        }
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid RegisterRequest registerRequest, BindingResult bindingResult) {
        ModelAndView modelAndView = new ModelAndView("register");


        // If validation fails, return register page
        if (bindingResult.hasErrors()) {
            return modelAndView;
        }

        try {
            userService.register(registerRequest);
            return new ModelAndView("redirect:/login"); // Redirect on success
        } catch (DomainException e) {
            if (e.getMessage().contains("Username")) {
                modelAndView.addObject("usernameError", e.getMessage());
            }
            if (e.getMessage().contains("Passwords do not match")) {
                modelAndView.addObject("passwordError", e.getMessage());
            }
            return modelAndView;
        }
    }


    @GetMapping("/home")
    public ModelAndView getHomePage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.getById(authenticationMetadata.getUserId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("home");
        modelAndView.addObject("user", user);

        //Get a random welcoming message with each load of the home page
        WelcomeMessage[] values = WelcomeMessage.values();
        Random r = new Random();
        String message = values[r.nextInt(values.length)].getMessage();
        modelAndView.addObject("message", message);

        return modelAndView;
    }

    @GetMapping("/hack-on")
    public ModelAndView getHackPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.getById(authenticationMetadata.getUserId());
        List<User> allUsersExceptMe = userService.getAllUsersExceptMe(authenticationMetadata.getUsername());
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("hack-on");
        modelAndView.addObject("user", user);
        modelAndView.addObject("allUsersExceptMe", allUsersExceptMe);
        return modelAndView;
    }

    @GetMapping("/scoreboard")
    public ModelAndView getScoreboard(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("scoreboard");
        List<User> allUsers = userService.getAllUsersOrdered();
        User user = userService.getByUsername(authenticationMetadata.getUsername());
        modelAndView.addObject("allUsers", allUsers);
        modelAndView.addObject("user", user);
        log.info("Passing view with all users");
        return modelAndView;
    }

    @GetMapping("/upgrades")
    public ModelAndView getUpgradePage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("upgrades");
        User user = userService.getByUsername(authenticationMetadata.getUsername());
        modelAndView.addObject("user", user);
        return modelAndView;
    }
}
