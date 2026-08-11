package euan.bookingservice.bookings.controller;

import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.bookings.entity.BookingStatus;
import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.entity.ResourceType;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser() {
        User user = new User();
        user.setUsername("user");
        user.setPassword("pw");
        user.setRole("USER");
        return userRepository.save(user);
    }

    private User createAnotherUser() {
        User user = new User();
        user.setUsername("anotherUser");
        user.setPassword("pw");
        user.setRole("USER");
        return userRepository.save(user);
    }

    private Resource createResource(User owner) {
        Resource resource = new Resource();
        resource.setTitle("Room A");
        resource.setDescription("Meeting room");
        resource.setResourceType(ResourceType.EVENT);
        resource.setOwnerId(owner.getId());
        return resourceRepository.save(resource);
    }

    private Booking createBooking(User user, Resource resource) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setResourceId(resource.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStartTime(OffsetDateTime.now());
        booking.setEndTime(OffsetDateTime.now().plusHours(1));
        return bookingRepository.save(booking);
    }

    @Test
    void createBooking_withoutAuthentication_returnsUnauthorized() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);

        String payload = """
                {
                  "resourceId":%d,
                  "userId":%d
                }
                """.formatted(resource.getId(), user.getId());

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBooking_asUser_forSelf_createsBooking() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);

        String payload = """
            {
              "resourceId":%d,
              "userId":%d,
              "startTime":"2026-08-15T10:00:00+01:00",
              "endTime":"2026-08-15T12:00:00+01:00"
            }
            """.formatted(resource.getId(), user.getId());

        mockMvc.perform(post("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId", is(resource.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(user.getId().intValue())));

        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    @Test
    void createBooking_asUser_forAnotherUser_returnsForbidden() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();
        Resource resource = createResource(user);

        String payload = """
                {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """.formatted(resource.getId(), anotherUser.getId());

        mockMvc.perform(post("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    @Test
    void createBooking_asAdmin_forAnotherUser_createsBooking() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();
        Resource resource = createResource(user);

        String payload = """
                {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """.formatted(resource.getId(), anotherUser.getId());

        mockMvc.perform(post("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId", is(resource.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(anotherUser.getId().intValue())));

        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    @Test
    void createBooking_whenResourceDoesNotExist_returnsBadRequest() throws Exception {

        User user = createUser();

        String payload = """
                {
                  "resourceId":999,
                  "userId":%d
                }
                """.formatted(user.getId());

        mockMvc.perform(post("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    @Test
    void createBooking_whenUserDoesNotExist_returnsBadRequest() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);

        String payload = """
                {
                  "resourceId":%d,
                  "userId":999
                }
                """.formatted(resource.getId());

        mockMvc.perform(post("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    @Test
    void getAllBookings_withoutAuthentication_returnsUnauthorized() throws Exception {

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllBookings_asUser_returnsOnlyOwnBookings() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();

        Resource resource = createResource(user);

        createBooking(user, resource);
        createBooking(anotherUser, resource);

        mockMvc.perform(get("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].userId", is(user.getId().intValue())))
                .andExpect(jsonPath("$[0].resourceId", is(resource.getId().intValue())));
    }

    @Test
    void getAllBookings_asUser_whenEmpty_returnsEmptyList() throws Exception {

        createUser();

        mockMvc.perform(get("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void getAllBookings_asAdmin_returnsAllBookings() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();

        Resource resource = createResource(user);

        createBooking(user, resource);
        createBooking(anotherUser, resource);

        mockMvc.perform(get("/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void getBookingById_withoutAuthentication_returnsUnauthorized() throws Exception {

        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBookingById_asUser_returnsOwnBooking() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);
        Booking booking = createBooking(user, resource);

        mockMvc.perform(get("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.resourceId", is(resource.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(user.getId().intValue())));
    }

    @Test
    void getBookingById_asUser_forAnotherUser_returnsForbidden() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();

        Resource resource = createResource(user);
        Booking booking = createBooking(anotherUser, resource);

        mockMvc.perform(get("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBookingById_asAdmin_returnsAnyBooking() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);
        Booking booking = createBooking(user, resource);

        mockMvc.perform(get("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(user.getId().intValue())));
    }

    @Test
    void getBookingById_whenMissing_returnsNotFound() throws Exception {

        createUser();

        mockMvc.perform(get("/bookings/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBooking_withoutAuthentication_returnsUnauthorized() throws Exception {

        String payload = """
                {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """;

        mockMvc.perform(put("/bookings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateBooking_asUser_forOwnBooking_updatesBooking() throws Exception {

        User user = createUser();

        Resource resource = createResource(user);
        Resource updatedResource = new Resource();
        updatedResource.setTitle("Room B");
        updatedResource.setDescription("Updated meeting room");
        updatedResource.setResourceType(ResourceType.EVENT);
        updatedResource.setOwnerId(user.getId());
        updatedResource = resourceRepository.save(updatedResource);

        Booking booking = createBooking(user, resource);

        String payload = """
                {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """.formatted(updatedResource.getId(), user.getId());

        mockMvc.perform(put("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.resourceId", is(updatedResource.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(user.getId().intValue())));

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(updatedBooking.getResourceId()).isEqualTo(updatedResource.getId());
        assertThat(updatedBooking.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void updateBooking_asUser_forAnotherUsersBooking_returnsForbidden() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();

        Resource resource = createResource(user);
        Booking booking = createBooking(anotherUser, resource);

        String payload = """
                 {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """.formatted(resource.getId(), user.getId());

        mockMvc.perform(put("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateBooking_asUser_cannotChangeBookingToAnotherUser_returnsForbidden() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();

        Resource resource = createResource(user);
        Booking booking = createBooking(user, resource);

        String payload = """
                 {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """.formatted(resource.getId(), anotherUser.getId());

        mockMvc.perform(put("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        Booking unchangedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(unchangedBooking.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void updateBooking_asAdmin_canUpdateAnyBooking() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();

        Resource resource = createResource(user);

        Booking booking = createBooking(user, resource);

        String payload = """
                 {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """.formatted(resource.getId(), anotherUser.getId());

        mockMvc.perform(put("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(anotherUser.getId().intValue())));
    }

    @Test
    void updateBooking_whenNotFound_returnsNotFound() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);

        String payload = """
                {
                  "resourceId":%d,
                  "userId":%d,
                  "startTime":"2026-08-15T10:00:00+01:00",
                  "endTime":"2026-08-15T12:00:00+01:00"
                }
                """.formatted(resource.getId(), user.getId());

        mockMvc.perform(put("/bookings/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBooking_withoutAuthentication_returnsUnauthorized() throws Exception {

        mockMvc.perform(delete("/bookings/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteBooking_asUser_forOwnBooking_deletesBooking() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);
        Booking booking = createBooking(user, resource);

        mockMvc.perform(delete("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isNoContent());

        assertThat(bookingRepository.findById(booking.getId())).isEmpty();
    }

    @Test
    void deleteBooking_asUser_forAnotherUsersBooking_returnsForbidden() throws Exception {

        User user = createUser();
        User anotherUser = createAnotherUser();

        Resource resource = createResource(user);
        Booking booking = createBooking(anotherUser, resource);

        mockMvc.perform(delete("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());

        assertThat(bookingRepository.findById(booking.getId())).isPresent();
    }

    @Test
    void deleteBooking_asAdmin_deletesAnyBooking() throws Exception {

        User user = createUser();
        Resource resource = createResource(user);
        Booking booking = createBooking(user, resource);

        mockMvc.perform(delete("/bookings/" + booking.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("ADMIN")))
                .andExpect(status().isNoContent());

        assertThat(bookingRepository.findById(booking.getId())).isEmpty();
    }

    @Test
    void deleteBooking_whenMissing_returnsNotFound() throws Exception {

        mockMvc.perform(delete("/bookings/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
