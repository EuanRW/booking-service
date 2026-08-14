package euan.bookingservice.authentication.model;

public record AuthenticationUser(
        String username,
        String encodedPassword,
        String role
) {
}