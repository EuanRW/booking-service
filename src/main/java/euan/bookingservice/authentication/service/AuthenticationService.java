package euan.bookingservice.authentication.service;

import euan.bookingservice.authentication.dto.AuthRequest;
import euan.bookingservice.authentication.dto.AuthResponse;
import euan.bookingservice.authentication.dto.RegisterRequest;
import euan.bookingservice.authentication.port.AuthenticationUserPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final String DEFAULT_ROLE = "USER";

    private final AuthenticationManager authenticationManager;
    private final AuthenticationUserPort authenticationUserPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            AuthenticationUserPort authenticationUserPort,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.authenticationUserPort = authenticationUserPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        return new AuthResponse(
                jwtService.generateToken(request.getUsername())
        );
    }

    public AuthResponse register(RegisterRequest request) {
        authenticationUserPort.registerUser(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                DEFAULT_ROLE
        );

        return new AuthResponse(
                jwtService.generateToken(request.getUsername())
        );
    }
}