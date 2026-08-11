package euan.bookingservice.bookings.controller;

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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
class BookingAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceAvailabilityRuleRepository availabilityRuleRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Resource resource;

    private static final LocalDate MONDAY =
            LocalDate.of(2026, 8, 17);

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        availabilityRuleRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        user = createUser();
        resource = createResource(user, 1);
    }

    /*
     * ------------------------------------------------------------
     * 1. Booking entirely inside availability -> accepted
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_entirelyInsideAvailability_returnsCreated()
            throws Exception {

        createAvailabilityRule(
                resource,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                null
        );

        performCreateBooking(
                resource,
                MONDAY.atTime(10, 0).atOffset(java.time.ZoneOffset.UTC),
                MONDAY.atTime(11, 0).atOffset(java.time.ZoneOffset.UTC)
        )
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    /*
     * ------------------------------------------------------------
     * 2. Booking starts before availability -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_startsBeforeAvailability_returnsConflict()
            throws Exception {

        createStandardMondayAvailability();

        performCreateBooking(
                resource,
                utc(MONDAY, 8, 30),
                utc(MONDAY, 10, 0)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /*
     * ------------------------------------------------------------
     * 3. Booking ends after availability -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_endsAfterAvailability_returnsConflict()
            throws Exception {

        createStandardMondayAvailability();

        performCreateBooking(
                resource,
                utc(MONDAY, 16, 0),
                utc(MONDAY, 17, 30)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /*
     * ------------------------------------------------------------
     * 4. Booking completely outside availability -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_completelyOutsideAvailability_returnsConflict()
            throws Exception {

        createStandardMondayAvailability();

        performCreateBooking(
                resource,
                utc(MONDAY, 18, 0),
                utc(MONDAY, 19, 0)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /*
     * ------------------------------------------------------------
     * 5. Booking on wrong day of week -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_onWrongDayOfWeek_returnsConflict()
            throws Exception {

        createStandardMondayAvailability();

        LocalDate tuesday = MONDAY.plusDays(1);

        performCreateBooking(
                resource,
                utc(tuesday, 10, 0),
                utc(tuesday, 11, 0)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /*
     * ------------------------------------------------------------
     * 6. Booking before effectiveFrom -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_beforeEffectiveFrom_returnsConflict()
            throws Exception {

        createAvailabilityRule(
                resource,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                MONDAY.plusWeeks(1),
                null
        );

        performCreateBooking(
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /*
     * ------------------------------------------------------------
     * 7. Booking after effectiveTo -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_afterEffectiveTo_returnsConflict()
            throws Exception {

        createAvailabilityRule(
                resource,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                MONDAY.minusWeeks(1)
        );

        performCreateBooking(
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /*
     * ------------------------------------------------------------
     * 8. Booking exactly matches availability boundaries -> accepted
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_exactlyMatchesAvailability_returnsCreated()
            throws Exception {

        createStandardMondayAvailability();

        performCreateBooking(
                resource,
                utc(MONDAY, 9, 0),
                utc(MONDAY, 17, 0)
        )
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    /*
     * ------------------------------------------------------------
     * 9. Adjacent booking at boundary -> accepted
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_adjacentToExistingBooking_returnsCreated()
            throws Exception {

        createStandardMondayAvailability();

        createExistingBooking(
                user,
                resource,
                utc(MONDAY, 9, 0),
                utc(MONDAY, 10, 0)
        );

        performCreateBooking(
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        )
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll()).hasSize(2);
    }

    /*
     * ------------------------------------------------------------
     * 10. Capacity reached -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_whenCapacityReached_returnsConflict()
            throws Exception {

        resource.setCapacity(1);
        resourceRepository.save(resource);

        createStandardMondayAvailability();

        createExistingBooking(
                user,
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        );

        performCreateBooking(
                resource,
                utc(MONDAY, 10, 15),
                utc(MONDAY, 10, 45)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    /*
     * ------------------------------------------------------------
     * 11. Capacity not reached -> accepted
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_whenCapacityNotReached_returnsCreated()
            throws Exception {

        resource.setCapacity(2);
        resourceRepository.save(resource);

        createStandardMondayAvailability();

        createExistingBooking(
                user,
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        );

        performCreateBooking(
                resource,
                utc(MONDAY, 10, 15),
                utc(MONDAY, 10, 45)
        )
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll()).hasSize(2);
    }

    /*
     * ------------------------------------------------------------
     * 12. capacity == null -> overlapping bookings accepted
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_whenCapacityIsNull_allowsOverlappingBooking()
            throws Exception {

        resource.setCapacity(null);
        resourceRepository.save(resource);

        createStandardMondayAvailability();

        createExistingBooking(
                user,
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 12, 0)
        );

        performCreateBooking(
                resource,
                utc(MONDAY, 10, 30),
                utc(MONDAY, 11, 30)
        )
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll()).hasSize(2);
    }

    /*
     * ------------------------------------------------------------
     * 13. capacity <= 0 -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void createBooking_whenCapacityIsZero_returnsConflict()
            throws Exception {

        resource.setCapacity(0);
        resourceRepository.save(resource);

        createStandardMondayAvailability();

        performCreateBooking(
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        )
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /*
     * ------------------------------------------------------------
     * 14. Update to valid interval -> accepted
     * ------------------------------------------------------------
     */

    @Test
    void updateBooking_toValidInterval_returnsOk()
            throws Exception {

        createStandardMondayAvailability();

        Booking booking = createExistingBooking(
                user,
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        );

        performUpdateBooking(
                booking,
                resource,
                utc(MONDAY, 12, 0),
                utc(MONDAY, 13, 0)
        )
                .andExpect(status().isOk());

        Booking updated =
                bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(updated.getStartTime())
                .isEqualTo(utc(MONDAY, 12, 0));

        assertThat(updated.getEndTime())
                .isEqualTo(utc(MONDAY, 13, 0));
    }

    /*
     * ------------------------------------------------------------
     * 15. Update to invalid interval -> rejected
     * ------------------------------------------------------------
     */

    @Test
    void updateBooking_toInvalidInterval_returnsConflict()
            throws Exception {

        createStandardMondayAvailability();

        Booking booking = createExistingBooking(
                user,
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        );

        performUpdateBooking(
                booking,
                resource,
                utc(MONDAY, 16, 30),
                utc(MONDAY, 18, 0)
        )
                .andExpect(status().isConflict());

        Booking unchanged =
                bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(unchanged.getStartTime())
                .isEqualTo(utc(MONDAY, 10, 0));

        assertThat(unchanged.getEndTime())
                .isEqualTo(utc(MONDAY, 11, 0));
    }

    /*
     * ------------------------------------------------------------
     * 16. Updating unchanged booking does not reject itself
     * ------------------------------------------------------------
     */

    @Test
    void updateBooking_withoutChangingInterval_doesNotRejectItself()
            throws Exception {

        resource.setCapacity(1);
        resourceRepository.save(resource);

        createStandardMondayAvailability();

        Booking booking = createExistingBooking(
                user,
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        );

        performUpdateBooking(
                booking,
                resource,
                utc(MONDAY, 10, 0),
                utc(MONDAY, 11, 0)
        )
                .andExpect(status().isOk());

        assertThat(bookingRepository.findAll()).hasSize(1);

        Booking updated =
                bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(updated.getStartTime())
                .isEqualTo(utc(MONDAY, 10, 0));

        assertThat(updated.getEndTime())
                .isEqualTo(utc(MONDAY, 11, 0));
    }

    /*
     * ============================================================
     * Helpers
     * ============================================================
     */

    private User createUser() {
        User user = new User();
        user.setUsername("user");
        user.setPassword("pw");
        user.setRole("USER");

        return userRepository.save(user);
    }

    private Resource createResource(
            User owner,
            Integer capacity
    ) {
        Resource resource = new Resource();

        resource.setTitle("Room A");
        resource.setDescription("Meeting room");
        resource.setResourceType(ResourceType.EVENT);
        resource.setOwnerId(owner.getId());
        resource.setCapacity(capacity);

        return resourceRepository.save(resource);
    }

    private ResourceAvailabilityRule createAvailabilityRule(
            Resource resource,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        ResourceAvailabilityRule rule =
                new ResourceAvailabilityRule();

        rule.setResource(resource);
        rule.setDayOfWeek(dayOfWeek);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setEffectiveFrom(effectiveFrom);
        rule.setEffectiveTo(effectiveTo);

        return availabilityRuleRepository.save(rule);
    }

    private void createStandardMondayAvailability() {
        createAvailabilityRule(
                resource,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                null
        );
    }

    private Booking createExistingBooking(
            User user,
            Resource resource,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) {
        Booking booking = new Booking();

        booking.setUser(user);
        booking.setResourceId(resource.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);

        return bookingRepository.save(booking);
    }

    private org.springframework.test.web.servlet.ResultActions
    performCreateBooking(
            Resource resource,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) throws Exception {

        String payload = """
                {
                  "resourceId": %d,
                  "userId": %d,
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(
                resource.getId(),
                user.getId(),
                startTime,
                endTime
        );

        return mockMvc.perform(
                post("/bookings")
                        .with(
                                SecurityMockMvcRequestPostProcessors
                                        .user("user")
                                        .roles("USER")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        );
    }

    private org.springframework.test.web.servlet.ResultActions
    performUpdateBooking(
            Booking booking,
            Resource resource,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) throws Exception {

        String payload = """
                {
                  "resourceId": %d,
                  "userId": %d,
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(
                resource.getId(),
                user.getId(),
                startTime,
                endTime
        );

        return mockMvc.perform(
                put("/bookings/" + booking.getId())
                        .with(
                                SecurityMockMvcRequestPostProcessors
                                        .user("user")
                                        .roles("USER")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        );
    }

    private OffsetDateTime utc(
            LocalDate date,
            int hour,
            int minute
    ) {
        return date
                .atTime(hour, minute)
                .atOffset(java.time.ZoneOffset.UTC);
    }
}