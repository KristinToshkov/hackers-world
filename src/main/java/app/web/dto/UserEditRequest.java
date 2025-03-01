package app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEditRequest {


    @Pattern(regexp = "(^$|.{6,})", message = "Username must be at least 6 symbols")
    private String username;

    @Pattern(regexp = "(^$|.{6,})", message = "Password must be at least 6 symbols")
    private String password;

    @URL(message = "Must be a valid URL")
    private String profilePicture;

    @Email(message = "Must be a valid e-mail")
    private String email;

}
