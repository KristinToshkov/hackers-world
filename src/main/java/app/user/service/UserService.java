package app.user.service;

import app.exception.DomainException;
import app.security.AuthenticationMetadata;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;

import app.web.dto.PasswordRequest;
import app.web.dto.RegisterRequest;
import app.web.dto.UserEditRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TransactionService transactionService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, TransactionService transactionService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionService = transactionService;
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
                .credits((double) 0)
                .userRank(0)
                .createdOn(LocalDateTime.now())
                .build();
    }

    public void decreaseCredits(User user, int amount) {
        user.setCredits(user.getCredits() - amount);
    }

    public void increaseCredits(User user, int amount) {
        user.setCredits(user.getCredits() + amount);
    }

    public void initializeRootUser(User user) {

        User user1 =  User.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .password(passwordEncoder.encode(user.getPassword()))
                .role(user.getRole())
                .isActive(user.isActive())
                .userRank(user.getUserRank())
                .credits(user.getCredits())
                .createdOn(user.getCreatedOn())
                .build();

        userRepository.save(user1);
    }


    @Cacheable("users")
    public List<User> getAllUsers() {

        log.info("Contacted DB");
        return userRepository.findAllByisActiveTrue();

    }

    @Cacheable("usersOrdered")
    public List<User> getAllUsersOrdered() {

        log.info("Contacted DB");
        return userRepository.findAllByisActiveTrueOrderByUserRankDesc();

    }

    @Cacheable("userHistory")
    public List<User> getUserHistory(String username) {

        log.info("Contacted DB");
        return userRepository.findAllByisActiveTrueOrderByUserRankDesc();

    }

    @Cacheable("usersExceptMe")
    public List<User> getAllUsersExceptMe(String username) {
        log.info("Contacted DB");
        List<User> all = userRepository.findAllByisActiveTrue();
        all.removeIf(u -> u.getUsername().equals(username));
        return all;

    }
    @Cacheable("usersExceptMeFull")
    public List<User> getAllUsersExceptMeFull(String username) {
        log.info("Contacted DB");
        List<User> all = userRepository.findAll();
        all.removeIf(u -> u.getUsername().equals(username));
        return all;

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

    @CacheEvict(value = {"users", "usersOrdered", "usersExceptMe"}, allEntries = true)
    public void editUserDetails(@Valid UUID id, UserEditRequest userEditRequest) {
        Optional<User> byId = userRepository.findById(id);
        User user = byId.orElseThrow(() -> new DomainException("User with this username does not exist."));
        if(!userEditRequest.getUsername().isEmpty())
            user.setUsername(userEditRequest.getUsername());

        user.setEmail(userEditRequest.getEmail());
        if(!Objects.equals(userEditRequest.getPassword(), ""))
            user.setPassword(passwordEncoder.encode(userEditRequest.getPassword()));
        user.setProfilePicture(userEditRequest.getProfilePicture());
        try {
        userRepository.save(user);
    }catch (Exception e) {
        throw new DomainException("Username is taken");
    }
    }

    public void logoutUser() {
        SecurityContextHolder.clearContext();

        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            }
        }

        log.info("User logged out. All caches cleared.");
    }

    public User getByUsername(String username) {
        return userRepository.getUsersByUsername(username);
    }

    @CacheEvict(value = "usersOrdered", allEntries = true)
    public void rankUpUser(User user) {
        if(user.getCredits() >= 50) {
            user.setCredits(user.getCredits() - 50);
            transactionService.createTransaction(user, 50.0, "Rank Up", TransactionType.SEND);
            user.setUserRank(user.getUserRank() + 1);
            userRepository.save(user);
        } else throw new DomainException("You need 50 credits to rank up!");
    }

    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    @CacheEvict(value = {"users", "usersOrdered", "usersExceptMe", "usersExceptMeFull"}, allEntries = true)
    public void banUser(User user) {
        user.setActive(false);
        userRepository.save(user);
    }

    @CacheEvict(value = {"users", "usersOrdered", "usersExceptMe", "usersExceptMeFull"}, allEntries = true)
    public void unbanUser(User user) {
        user.setActive(true);
        userRepository.save(user);
    }

    @CacheEvict(value = {"users", "usersOrdered", "usersExceptMe", "usersExceptMeFull"}, allEntries = true)
    public void promoteUser(User user) {
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
    }

    @CacheEvict(value = {"users", "usersOrdered", "usersExceptMe", "usersExceptMeFull"}, allEntries = true)
    public void demoteUser(User user) {
        user.setRole(UserRole.USER);
        userRepository.save(user);
    }
}
