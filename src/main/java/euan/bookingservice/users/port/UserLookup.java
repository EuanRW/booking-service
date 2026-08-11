package euan.bookingservice.users.port;

import java.util.Optional;

public interface UserLookup {
    boolean existsById(Long userId);
    Optional<Long> findUserIdByUsername(String username);
}
