package euan.bookingservice.seed;

import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.repository.ResourceAvailabilityRuleRepository;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityRuleRepository resourceAvailabilityRuleRepository;
    private final BookingRepository bookingRepository;

    private final DemoDataFactory factory;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.count() > 0) {
            return;
        }

        Authentication systemAuth = new UsernamePasswordAuthenticationToken(
                "system", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))
        );
        SecurityContextHolder.getContext().setAuthentication(systemAuth);

        // Users
        User admin = factory.admin();
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin = userRepository.save(admin);

        User alice = factory.user("alice@example.com");
        alice.setPassword(passwordEncoder.encode(alice.getPassword()));
        alice = userRepository.save(alice);

        User bob = factory.user("bob@example.com");
        bob.setPassword(passwordEncoder.encode(bob.getPassword()));
        bob = userRepository.save(bob);

        // Resources
        Resource classA = resourceRepository.save(
                factory.event("Hot Yoga", admin));

        Resource classB = resourceRepository.save(
                factory.event("Pilates", admin));

        // Availability rules
        resourceAvailabilityRuleRepository.save(
          factory.availabilityRule(
                  classA
          )
        );

        resourceAvailabilityRuleRepository.save(
                factory.availabilityRule(
                        classB
                )
        );

        // Bookings
        bookingRepository.save(factory.booking(
                alice,
                classA
        ));

        bookingRepository.save(factory.booking(
                bob,
                classB
        ));
        log.info("Demo data loaded.");
        System.out.println("Demo data loaded.");
    }
}
