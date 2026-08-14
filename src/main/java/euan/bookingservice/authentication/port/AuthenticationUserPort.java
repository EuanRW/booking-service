package euan.bookingservice.authentication.port;

import euan.bookingservice.authentication.model.AuthenticationUser;

import java.util.Optional;

public interface AuthenticationUserPort {

    Optional<AuthenticationUser> findByUsername(String username);

    void registerUser(
            String username,
            String encodedPassword,
            String role
    );
}