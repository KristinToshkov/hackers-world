package app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @Email(message = "Must be a valid e-mail")
    private String email;

    @Size(min = 6, message = "Username must be at least 6 symbols")
    private String username;

    @Size(min = 6, message = "Password must be at least 6 symbols")
    private String password;

    @Size(min = 6, message = "Passwords do not match")
    private String confirmPassword;
}
