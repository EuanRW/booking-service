package euan.bookingservice.resources.controller;

import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.bookings.entity.BookingStatus;
import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.entity.ResourceAvailabilityRule;
import euan.bookingservice.resources.entity.ResourceType;
import euan.bookingservice.resources.repository.ResourceAvailabilityRuleRepository;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
class AvailabilityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceAvailabilityRuleRepository availabilityRuleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        availabilityRuleRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createOwner() {
        User user = new User();
        user.setUsername("organizer");
        user.setPassword("pw");
        user.setRole("ORGANIZER");
        return userRepository.save(user);
    }

    private User createSecondOwner() {
        User user = new User();
        user.setUsername("other-organizer");
        user.setPassword("pw");
        user.setRole("ORGANIZER");
        return userRepository.save(user);
    }

    private Resource createResource(User owner) {
        Resource resource = new Resource();
        resource.setTitle("Room A");
        resource.setDescription("Meeting room");
        resource.setResourceType(ResourceType.EVENT);
        resource.setOwnerId(owner.getId());
        resource.setCapacity(1);
        return resourceRepository.save(resource);
    }

    private ResourceAvailabilityRule createRule(
            Resource resource,
            DayOfWeek dayOfWeek,
            String start,
            String end
    ) {
        ResourceAvailabilityRule rule = new ResourceAvailabilityRule();
        rule.setResource(resource);
        rule.setDayOfWeek(dayOfWeek);
        rule.setStartTime(LocalTime.parse(start));
        rule.setEndTime(LocalTime.parse(end));

        return availabilityRuleRepository.save(rule);
    }

    private Booking createBooking(
            Resource resource,
            String start,
            String end
    ) {
        Booking booking = new Booking();
        booking.setResourceId(resource.getId());
        booking.setUser(createBookingUser());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStartTime(
                OffsetDateTime.parse(start)
        );
        booking.setEndTime(
                OffsetDateTime.parse(end)
        );

        return bookingRepository.save(booking);
    }

    private User createBookingUser() {
        User user = new User();
        user.setUsername("booker-" + System.nanoTime());
        user.setPassword("pw");
        user.setRole("USER");

        return userRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // CREATE RULE
    // -------------------------------------------------------------------------

    @Test
    void createAvailabilityRule_withoutAuthentication_returnsUnauthorized() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00",
                  "endTime": "17:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAvailabilityRule_asUser_returnsForbidden() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00",
                  "endTime": "17:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void createAvailabilityRule_asOwner_createsRule() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00",
                  "endTime": "17:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId",
                        is(resource.getId().intValue())))
                .andExpect(jsonPath("$.dayOfWeek",
                        is("MONDAY")))
                .andExpect(jsonPath("$.startTime",
                        is("09:00:00")))
                .andExpect(jsonPath("$.endTime",
                        is("17:00:00")));

        assertThat(
                availabilityRuleRepository.findByResourceId(resource.getId())
        ).hasSize(1);
    }

    @Test
    void createAvailabilityRule_asAdmin_createsRule() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "TUESDAY",
                  "startTime": "10:00:00",
                  "endTime": "12:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("admin")
                                                .roles("ADMIN")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isCreated());

        assertThat(
                availabilityRuleRepository.findByResourceId(resource.getId())
        ).hasSize(1);
    }

    @Test
    void createAvailabilityRule_asDifferentOrganizer_returnsForbidden() throws Exception {

        User owner = createOwner();
        createSecondOwner();

        Resource resource = createResource(owner);

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00",
                  "endTime": "17:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("other-organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void createAvailabilityRule_withInvalidTimes_returnsBadRequest() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "MONDAY",
                  "startTime": "17:00:00",
                  "endTime": "09:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAvailabilityRule_withOverlappingRule_returnsBadRequest() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        createRule(
                resource,
                DayOfWeek.MONDAY,
                "09:00",
                "12:00"
        );

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "MONDAY",
                  "startTime": "11:00:00",
                  "endTime": "14:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isBadRequest());

        assertThat(
                availabilityRuleRepository.findByResourceId(resource.getId())
        ).hasSize(1);
    }

    @Test
    void createAvailabilityRule_adjacentRules_areAllowed() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        createRule(
                resource,
                DayOfWeek.MONDAY,
                "09:00",
                "12:00"
        );

        String payload = """
                {
                  "resourceId": %d,
                  "dayOfWeek": "MONDAY",
                  "startTime": "12:00:00",
                  "endTime": "14:00:00"
                }
                """.formatted(resource.getId());

        mockMvc.perform(
                        post("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isCreated());

        assertThat(
                availabilityRuleRepository.findByResourceId(resource.getId())
        ).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // GET RULES
    // -------------------------------------------------------------------------

    @Test
    void getAvailabilityRules_asUser_returnsRules() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        createRule(
                resource,
                DayOfWeek.MONDAY,
                "09:00",
                "17:00"
        );

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].dayOfWeek",
                        is("MONDAY")))
                .andExpect(jsonPath("$[0].startTime",
                        is("09:00:00")))
                .andExpect(jsonPath("$[0].endTime",
                        is("17:00:00")));
    }

    @Test
    void getAvailabilityRules_whenEmpty_returnsEmptyList() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability/rules")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void getAvailabilityRules_withoutAuthentication_returnsUnauthorized() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability/rules")
                )
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // UPDATE RULE
    // -------------------------------------------------------------------------

    @Test
    void updateAvailabilityRule_asOwner_updatesRule() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        ResourceAvailabilityRule rule =
                createRule(
                        resource,
                        DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"
                );

        String payload = """
                {
                  "dayOfWeek": "TUESDAY",
                  "startTime": "10:00:00",
                  "endTime": "18:00:00"
                }
                """;

        mockMvc.perform(
                        put("/resources/" + resource.getId()
                                + "/availability/rules/" + rule.getId())
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek",
                        is("TUESDAY")))
                .andExpect(jsonPath("$.startTime",
                        is("10:00:00")))
                .andExpect(jsonPath("$.endTime",
                        is("18:00:00")));

        ResourceAvailabilityRule updated =
                availabilityRuleRepository
                        .findById(rule.getId())
                        .orElseThrow();

        assertThat(updated.getDayOfWeek())
                .isEqualTo(DayOfWeek.TUESDAY);
        assertThat(updated.getStartTime())
                .isEqualTo(LocalTime.of(10, 0));
        assertThat(updated.getEndTime())
                .isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void updateAvailabilityRule_asDifferentOrganizer_returnsForbidden() throws Exception {

        User owner = createOwner();
        createSecondOwner();

        Resource resource = createResource(owner);

        ResourceAvailabilityRule rule =
                createRule(
                        resource,
                        DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"
                );

        String payload = """
                {
                  "dayOfWeek": "TUESDAY",
                  "startTime": "10:00:00",
                  "endTime": "18:00:00"
                }
                """;

        mockMvc.perform(
                        put("/resources/" + resource.getId()
                                + "/availability/rules/" + rule.getId())
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("other-organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAvailabilityRule_whenMissing_returnsNotFound() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00",
                  "endTime": "17:00:00"
                }
                """;

        mockMvc.perform(
                        put("/resources/" + resource.getId()
                                + "/availability/rules/999")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAvailabilityRule_fromDifferentResource_returnsNotFound() throws Exception {

        User owner = createOwner();
        Resource resourceA = createResource(owner);

        Resource resourceB = createResource(owner);

        ResourceAvailabilityRule rule =
                createRule(
                        resourceB,
                        DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"
                );

        String payload = """
                {
                  "dayOfWeek": "TUESDAY",
                  "startTime": "10:00:00",
                  "endTime": "18:00:00"
                }
                """;

        mockMvc.perform(
                        put("/resources/" + resourceA.getId()
                                + "/availability/rules/" + rule.getId())
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE RULE
    // -------------------------------------------------------------------------

    @Test
    void deleteAvailabilityRule_asOwner_deletesRule() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        ResourceAvailabilityRule rule =
                createRule(
                        resource,
                        DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"
                );

        mockMvc.perform(
                        delete("/resources/" + resource.getId()
                                + "/availability/rules/" + rule.getId())
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                )
                .andExpect(status().isNoContent());

        assertThat(
                availabilityRuleRepository.findById(rule.getId())
        ).isEmpty();
    }

    @Test
    void deleteAvailabilityRule_asUser_returnsForbidden() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        ResourceAvailabilityRule rule =
                createRule(
                        resource,
                        DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"
                );

        mockMvc.perform(
                        delete("/resources/" + resource.getId()
                                + "/availability/rules/" + rule.getId())
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                )
                .andExpect(status().isForbidden());

        assertThat(
                availabilityRuleRepository.findById(rule.getId())
        ).isPresent();
    }

    @Test
    void deleteAvailabilityRule_whenMissing_returnsNotFound() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(
                        delete("/resources/" + resource.getId()
                                + "/availability/rules/999")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("organizer")
                                                .roles("ORGANIZER")
                                )
                )
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // CALCULATED AVAILABILITY
    // -------------------------------------------------------------------------

    @Test
    void getAvailability_returnsConfiguredAvailability() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        createRule(
                resource,
                DayOfWeek.MONDAY,
                "09:00",
                "17:00"
        );

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                                .param(
                                        "from",
                                        "2026-08-10T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-08-10T23:59:59Z"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()", is(1)))
                .andExpect(jsonPath(
                        "$.slots[0].startTime",
                        is("2026-08-10T09:00:00Z")
                ))
                .andExpect(jsonPath(
                        "$.slots[0].endTime",
                        is("2026-08-10T17:00:00Z")
                ));
    }

    @Test
    void getAvailability_subtractsConfirmedBooking() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        createRule(
                resource,
                DayOfWeek.MONDAY,
                "09:00",
                "17:00"
        );

        createBooking(
                resource,
                "2026-08-10T12:00:00Z",
                "2026-08-10T13:00:00Z"
        );

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                                .param(
                                        "from",
                                        "2026-08-10T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-08-10T23:59:59Z"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()", is(2)))
                .andExpect(jsonPath(
                        "$.slots[0].startTime",
                        is("2026-08-10T09:00:00Z")
                ))
                .andExpect(jsonPath(
                        "$.slots[0].endTime",
                        is("2026-08-10T12:00:00Z")
                ))
                .andExpect(jsonPath(
                        "$.slots[1].startTime",
                        is("2026-08-10T13:00:00Z")
                ))
                .andExpect(jsonPath(
                        "$.slots[1].endTime",
                        is("2026-08-10T17:00:00Z")
                ));
    }

    @Test
    void getAvailability_ignoresCancelledBooking() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        createRule(
                resource,
                DayOfWeek.MONDAY,
                "09:00",
                "17:00"
        );

        Booking booking = new Booking();
        booking.setResourceId(resource.getId());
        booking.setUser(createBookingUser());
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStartTime(
                OffsetDateTime.of(
                        2026, 8, 10,
                        12, 0, 0, 0,
                        ZoneOffset.UTC
                )
        );
        booking.setEndTime(
                OffsetDateTime.of(
                        2026, 8, 10,
                        13, 0, 0, 0,
                        ZoneOffset.UTC
                )
        );

        bookingRepository.save(booking);

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                                .param(
                                        "from",
                                        "2026-08-10T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-08-10T23:59:59Z"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()", is(1)))
                .andExpect(jsonPath(
                        "$.slots[0].startTime",
                        is("2026-08-10T09:00:00Z")
                ))
                .andExpect(jsonPath(
                        "$.slots[0].endTime",
                        is("2026-08-10T17:00:00Z")
                ));
    }

    @Test
    void getAvailability_withoutAuthentication_returnsUnauthorized() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability")
                                .param(
                                        "from",
                                        "2026-08-10T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-08-10T23:59:59Z"
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAvailability_whenNoRules_returnsEmptySlots() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                                .param(
                                        "from",
                                        "2026-08-10T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-08-10T23:59:59Z"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()", is(0)));
    }

    @Test
    void getAvailability_whenFromIsAfterTo_returnsBadRequest() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(
                        get("/resources/" + resource.getId() + "/availability")
                                .with(
                                        SecurityMockMvcRequestPostProcessors
                                                .user("user")
                                                .roles("USER")
                                )
                                .param(
                                        "from",
                                        "2026-08-11T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-08-10T00:00:00Z"
                                )
                )
                .andExpect(status().isBadRequest());
    }
}