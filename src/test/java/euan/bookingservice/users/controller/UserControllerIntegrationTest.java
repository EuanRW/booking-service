package euan.bookingservice.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.repository.UserRepository;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.bookings.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // remove dependent bookings -> resources -> users to avoid FK constraint violations
        bookingRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getAllUsers_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllUsers_asMockUser_returnsUsers() throws Exception {
        User alice = new User();
        alice.setUsername("alice");
        alice.setPassword("pw");
        alice.setRole("USER");

        User bob = new User();
        bob.setUsername("bob");
        bob.setPassword("pw");
        bob.setRole("ORGANIZER");

        userRepository.save(alice);
        userRepository.save(bob);

        mockMvc.perform(get("/users")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].username", is("alice")))
                .andExpect(jsonPath("$[0].role", is("USER")))
                .andExpect(jsonPath("$[1].username", is("bob")))
                .andExpect(jsonPath("$[1].role", is("ORGANIZER")));
    }

    @Test
    void getAllUsers_whenNoUsersExist_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/users")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void getCurrentUser_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_asMockUser_returnsUser() throws Exception {
        // create user in repository
        User user = new User();
        user.setUsername("alice");
        user.setPassword("pw");
        user.setRole("USER");
        userRepository.save(user);

        // perform request with a mock authenticated user named alice
        mockMvc.perform(get("/users/me").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void getUserByUsername_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/by-username/bob"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserByUsername_asMockUser_returnsUser() throws Exception {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("pw");
        user.setRole("ORGANIZER");
        userRepository.save(user);

        mockMvc.perform(get("/users/by-username/bob").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("any")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("bob")))
                .andExpect(jsonPath("$.role", is("ORGANIZER")));
    }

    @Test
    void updateUser_withoutAuthentication_returnsUnauthorized() throws Exception {
        String payload = "{\"username\": \"charlie-updated\", \"password\": \"pw\", \"role\": \"ADMIN\"}";

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUser_asNonAdmin_returnsForbidden() throws Exception {
        User user = new User();
        user.setUsername("charlie");
        user.setPassword("pw");
        user.setRole("USER");
        User saved = userRepository.save(user);

        String payload = "{\"username\": \"charlie-updated\", \"password\": \"pw\", \"role\": \"ADMIN\"}";

        mockMvc.perform(put("/users/" + saved.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_asAdmin_updates() throws Exception {
        User user = new User();
        user.setUsername("charlie");
        user.setPassword("pw");
        user.setRole("USER");
        User saved = userRepository.save(user);

        String payload = "{\"username\": \"charlie-updated\", \"password\": \"pw\", \"role\": \"ADMIN\"}";

        mockMvc.perform(put("/users/" + saved.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("charlie-updated")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    void deleteUser_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_asNonAdmin_returnsForbidden() throws Exception {
        User user = new User();
        user.setUsername("dave");
        user.setPassword("pw");
        user.setRole("USER");
        User saved = userRepository.save(user);

        mockMvc.perform(delete("/users/" + saved.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asAdmin_deletes() throws Exception {
        User user = new User();
        user.setUsername("dave");
        user.setPassword("pw");
        user.setRole("USER");
        User saved = userRepository.save(user);

        mockMvc.perform(delete("/users/" + saved.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        // repository should no longer contain the user
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(saved.getId())).isEmpty();
    }
}
