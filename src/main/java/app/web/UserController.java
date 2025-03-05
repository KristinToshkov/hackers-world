package app.web;


import app.exception.DomainException;
import app.hack.model.Hack;
import app.hack.service.HackService;
import app.security.AuthenticationMetadata;
import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.web.Mapper.DtoMapper;
import app.web.dto.UserEditRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final HackService hackService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserService userService, HackService hackService, TransactionService transactionService, UserRepository userRepository) {
        this.userService = userService;
        this.hackService = hackService;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ModelAndView getProfileMenu(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        UUID id = authenticationMetadata.getUserId();
        User user = userService.getById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("settings");
        modelAndView.addObject("user", user);
        modelAndView.addObject("userEditRequest", DtoMapper.mapUserToUserEditRequest(user));

        return modelAndView;
    }

    @PutMapping("/profile")
    public ModelAndView updateUserProfile(@Valid UserEditRequest userEditRequest, BindingResult bindingResult, @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        UUID id = authenticationMetadata.getUserId();
        if (bindingResult.hasErrors()) {
            User user = userService.getById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("settings");
            modelAndView.addObject("user", user);
            modelAndView.addObject("userEditRequest", userEditRequest);
            return modelAndView;
        }


        try {
            userService.editUserDetails(id, userEditRequest);
            if(!userEditRequest.getUsername().isEmpty())
                authenticationMetadata.setUsername(userEditRequest.getUsername());
        } catch (DomainException e) {
            User user = userService.getById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("settings");
            modelAndView.addObject("user", user);
            modelAndView.addObject("userEditRequest", userEditRequest);
            modelAndView.addObject("usernameError", e.getMessage());
            return modelAndView;
        }
        return new ModelAndView("redirect:/home");
    }

    @GetMapping("/profile/{id}")
    public ModelAndView getEditUser(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata, @PathVariable UUID id) {
        User admin = userService.getById(authenticationMetadata.getUserId());
        if(!userService.isAdmin(admin)) {
            return new ModelAndView("redirect:/");
        }
        User user = userService.getById(id);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("edit-user");
        modelAndView.addObject("user", user);
        modelAndView.addObject("userEditRequest", DtoMapper.mapUserToUserEditRequest(user));
        return modelAndView;
    }

    @PutMapping("/profile/{id}")
    public ModelAndView updateUserProfile(@Valid UserEditRequest userEditRequest, BindingResult bindingResult, @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata, @PathVariable UUID id) {

        User admin = userService.getById(authenticationMetadata.getUserId());
        if(!userService.isAdmin(admin)) {
            return new ModelAndView("redirect:/");
        }
        if (bindingResult.hasErrors()) {
            User user = userService.getById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("edit-user");
            modelAndView.addObject("user", user);
            modelAndView.addObject("userEditRequest", userEditRequest);
            return modelAndView;
        }


        try {
            userService.editUserDetails(id, userEditRequest);
        } catch (DomainException e) {
            User user = userService.getById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("edit-user");
            modelAndView.addObject("user", user);
            modelAndView.addObject("userEditRequest", userEditRequest);
            modelAndView.addObject("usernameError", e.getMessage());
            return modelAndView;
        }
        return new ModelAndView("redirect:/dashboard");
    }

    @GetMapping("/ban/{id}")
    public ModelAndView banUser(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata, @PathVariable UUID id) {
        User user = userService.getById(authenticationMetadata.getUserId());
        if(!userService.isAdmin(user))
        {
            ModelAndView modelAndView = new ModelAndView("redirect:/home");
            modelAndView.addObject("user", user);
            return modelAndView;
        }
        User userToBan = userService.getById(id);
        userService.banUser(userToBan);
        List<User> allUsersExceptMe = userService.getAllUsersExceptMe(authenticationMetadata.getUsername());
        List<Transaction> allTransactions = transactionService.getAllTransactions();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/dashboard");
        modelAndView.addObject("user", user);
        modelAndView.addObject("allUsersExceptMe", allUsersExceptMe);
        modelAndView.addObject("allTransactions", allTransactions);
        return modelAndView;
    }

    @GetMapping("/unban/{id}")
    public ModelAndView unbanUser(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata, @PathVariable UUID id) {
        User user = userService.getById(authenticationMetadata.getUserId());
        if(!userService.isAdmin(user))
        {
            ModelAndView modelAndView = new ModelAndView("redirect:/home");
            modelAndView.addObject("user", user);
            return modelAndView;
        }
        User userToUnban = userService.getById(id);
        userService.unbanUser(userToUnban);
        List<User> allUsersExceptMe = userService.getAllUsersExceptMe(authenticationMetadata.getUsername());
        List<Transaction> allTransactions = transactionService.getAllTransactions();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/dashboard");
        modelAndView.addObject("user", user);
        modelAndView.addObject("allUsersExceptMe", allUsersExceptMe);
        modelAndView.addObject("allTransactions", allTransactions);
        return modelAndView;
    }

    @GetMapping("/promote/{id}")
    public ModelAndView promoteUser(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata, @PathVariable UUID id) {
        User user = userService.getById(authenticationMetadata.getUserId());
        if(!userService.isAdmin(user))
        {
            ModelAndView modelAndView = new ModelAndView("redirect:/home");
            modelAndView.addObject("user", user);
            return modelAndView;
        }
        User userToPromote = userService.getById(id);
        userService.promoteUser(userToPromote);
        List<User> allUsersExceptMe = userService.getAllUsersExceptMe(authenticationMetadata.getUsername());
        List<Transaction> allTransactions = transactionService.getAllTransactions();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/dashboard");
        modelAndView.addObject("user", user);
        modelAndView.addObject("allUsersExceptMe", allUsersExceptMe);
        modelAndView.addObject("allTransactions", allTransactions);
        return modelAndView;
    }

    @GetMapping("/demote/{id}")
    public ModelAndView demoteUser(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata, @PathVariable UUID id) {
        User user = userService.getById(authenticationMetadata.getUserId());
        if(!userService.isAdmin(user))
        {
            ModelAndView modelAndView = new ModelAndView("redirect:/home");
            modelAndView.addObject("user", user);
            return modelAndView;
        }
        User userToDemote = userService.getById(id);
        userService.demoteUser(userToDemote);
        List<User> allUsersExceptMe = userService.getAllUsersExceptMe(authenticationMetadata.getUsername());
        List<Transaction> allTransactions = transactionService.getAllTransactions();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/dashboard");
        modelAndView.addObject("user", user);
        modelAndView.addObject("allUsersExceptMe", allUsersExceptMe);
        modelAndView.addObject("allTransactions", allTransactions);
        return modelAndView;
    }

}
