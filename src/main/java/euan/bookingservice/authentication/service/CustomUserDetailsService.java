package euan.bookingservice.authentication.service;

import euan.bookingservice.authentication.model.AuthenticationUser;
import euan.bookingservice.authentication.port.AuthenticationUserPort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthenticationUserPort authenticationUserPort;

    public CustomUserDetailsService(
            AuthenticationUserPort authenticationUserPort
    ) {
        this.authenticationUserPort = authenticationUserPort;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AuthenticationUser user = authenticationUserPort
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username
                        )
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.username())
                .password(user.encodedPassword())
                .authorities(
                        new SimpleGrantedAuthority("ROLE_" + user.role())
                )
                .build();
    }
}