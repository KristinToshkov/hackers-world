package app.user.service;

import app.exception.DomainException;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;

import app.web.dto.PasswordRequest;
import app.web.dto.RegisterRequest;
import app.web.dto.UserEditRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public User register(RegisterRequest registerRequest) {

        Optional<User> optionUser = userRepository.findByUsername(registerRequest.getUsername());
        if (optionUser.isPresent()) {
            throw new DomainException("Username \"%s\" unavailable.".formatted(registerRequest.getUsername()));
        }
        if(!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new DomainException("Passwords do not match.");
        }

        User user = userRepository.save(initializeUser(registerRequest));

        log.info("Successfully create new user account for username [%s] and id [%s]".formatted(user.getUsername(), user.getId()));

        return user;
    }




    private User initializeUser(RegisterRequest registerRequest) {

        return User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build();
    }

    public void initializeRootUser(User user) {

        User user1 =  User.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .password(passwordEncoder.encode(user.getPassword()))
                .role(user.getRole())
                .isActive(user.isActive())
                .createdOn(user.getCreatedOn())
                .build();

        userRepository.save(user1);
    }

    // В началото се изпълнява веднъж този метод и резултата се пази в кеш
    // Всяко следващо извикване на този метод ще се чете резултата от кеша и няма да се извиква четенето от базата
    @Cacheable("users")
    public List<User> getAllUsers() {

        return userRepository.findAll();
    }

    public User getById(UUID id) {

        return userRepository.findById(id).orElseThrow(() -> new DomainException("User with id [%s] does not exist.".formatted(id)));
    }

    @CacheEvict(value = "users", allEntries = true)
    public void switchStatus(UUID userId) {

        User user = getById(userId);

        // НАЧИН 1:
//        if (user.isActive()){
//            user.setActive(false);
//        } else {
//            user.setActive(true);
//        }

        // false -> true
        // true -> false
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void switchRole(UUID userId) {

        User user = getById(userId);

        if (user.getRole() == UserRole.USER) {
            user.setRole(UserRole.ADMIN);
        } else {
            user.setRole(UserRole.USER);
        }

        userRepository.save(user);
    }

//     Всеки пък, когато потребител се логва, Spring Security ще извиква този метод
//     за да вземе детайлите на потребителя с този username
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new DomainException("User with this username does not exist."));

        return new AuthenticationMetadata(user.getId(), username, user.getPassword(), user.getRole(), user.isActive());
    }

    public void resetPassword(@Valid PasswordRequest passwordRequest) {
        User user = userRepository.findByUsername(passwordRequest.getUsername()).orElse(null);
        if(user == null) {
            throw new DomainException("User with this username does not exist.");
        }
        user.setPassword(passwordEncoder.encode("000000"));
        userRepository.save(user);
    }

    public void editUserDetails(@Valid UUID id, UserEditRequest userEditRequest) {
        Optional<User> byId = userRepository.findById(id);
        User user = byId.orElseThrow(() -> new DomainException("User with this username does not exist."));
        user.setUsername(userEditRequest.getUsername());
        user.setEmail(userEditRequest.getEmail());
        if(!Objects.equals(userEditRequest.getPassword(), ""))
            user.setPassword(passwordEncoder.encode(userEditRequest.getPassword()));
        user.setProfilePicture(userEditRequest.getProfilePicture());
        userRepository.save(user);
    }
}
