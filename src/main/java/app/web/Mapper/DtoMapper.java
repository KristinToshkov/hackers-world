package app.web.Mapper;

import app.hack.model.Hack;
import app.user.model.User;
import app.web.dto.HackRequest;
import app.web.dto.UserEditRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {

    public static UserEditRequest mapUserToUserEditRequest(User user) {

        return UserEditRequest.builder()
                .username(user.getUsername())
                .password("")
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .build();
    }

    public static HackRequest mapHackToHackRequest(Hack hack) {

        return HackRequest.builder()
                .credits(hack.getCredits())
                .build();
    }


}
