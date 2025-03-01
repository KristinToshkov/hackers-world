package app.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HackRequest {

    @Min(value = 1, message = "Minimum amount is 1")
    @Max(value = 50, message = "Maximum amount is 50")
    private Integer credits;

}
