package euan.lessonbookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Username is required")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "STUDENT|TEACHER", message = "Role must be either STUDENT or TEACHER")
    private String role;
}