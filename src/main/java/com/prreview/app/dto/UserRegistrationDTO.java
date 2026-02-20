package com.prreview.app.dto;

import com.prreview.app.enums.Role;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @NotNull(message = "Role is required")
    private Role role;

    @AssertTrue(message = "Passwords must match")
    public boolean isPasswordMatch() {
        return password == null && confirmPassword == null || password != null && password.equals(confirmPassword);
    }
}
