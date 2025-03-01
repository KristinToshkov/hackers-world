package app.user.service;

import app.user.model.User;
import app.user.model.UserRole;
import app.web.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserInit implements CommandLineRunner {

    private final UserService userService;

    @Autowired
    public UserInit(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {

        if (!userService.getAllUsers().isEmpty()) {
           return;
        }


        User user = User.builder()
                .email("kris@gmail.com")
                .username("KrisRoot")
                .password("123123")
                .createdOn(LocalDateTime.now())
                .userRank(999)
                .isActive(true)
                .role(UserRole.ADMIN)
                .build();

        userService.initializeRootUser(user);
    }
}
