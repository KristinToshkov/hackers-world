package app.security;

import app.user.model.UserRole;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// ТОЗИ КЛАС ПАЗИ ДАННИТЕ НА ЛОГНАТИЯ ПОТРЕБИТЕЛ
@Data
@Getter
@Setter
@AllArgsConstructor
@Builder
public class AuthenticationMetadata implements UserDetails {

    private UUID userId;
    private String username;
    private String password;
    private UserRole role;
    private boolean isActive;

    // Този метод се използва от Spring Security за да се разбере какви roles/authorities потребителя има
    // Authority - permission или роля
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        // hasRole("ADMIN") -> "ROLE_ADMIN"
        // hasAuthority("ADMIN") -> "ADMIN"

        // Permission: CAN_EDIT_USER_PROFILES

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

        return List.of(authority);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isActive;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
