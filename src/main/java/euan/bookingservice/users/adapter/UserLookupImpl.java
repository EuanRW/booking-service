package euan.bookingservice.users.adapter;

import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.port.UserLookup;
import euan.bookingservice.users.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserLookupImpl implements UserLookup {

    private final UserRepository userRepository;

    public UserLookupImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public Optional<Long> findUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId);
    }
}
