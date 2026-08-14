package euan.bookingservice.users.adapter;

import euan.bookingservice.authentication.model.AuthenticationUser;
import euan.bookingservice.authentication.port.AuthenticationUserPort;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.exception.UsernameAlreadyExistsException;
import euan.bookingservice.users.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticationUserAdapter implements AuthenticationUserPort {

    private final UserRepository userRepository;

    public AuthenticationUserAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<AuthenticationUser> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new AuthenticationUser(
                        user.getUsername(),
                        user.getPassword(),
                        user.getRole()
                ));
    }

    @Override
    public void registerUser(
            String username,
            String encodedPassword,
            String role
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setCreatedBy(username);
        user.setUpdatedBy(username);

        userRepository.save(user);
    }
}