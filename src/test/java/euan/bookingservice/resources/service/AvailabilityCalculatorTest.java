package euan.bookingservice.resources.service;

import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.resources.dto.response.AvailabilityResponse;
import euan.bookingservice.resources.dto.response.AvailabilitySlot;
import euan.bookingservice.resources.entity.ResourceAvailabilityRule;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityCalculatorTest {

    private final AvailabilityCalculator calculator =
            new AvailabilityCalculator();

    private static final LocalDate MONDAY =
            LocalDate.of(2026, 8, 10);

    // -------------------------------------------------------------------------
    // Basic availability
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnFullAvailabilityWhenThereAreNoBookings() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    @Test
    void shouldReturnNoAvailabilityWhenCapacityIsZero() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(),
                0
        );

        assertThat(result.getSlots()).isEmpty();
    }

    @Test
    void shouldReturnNoAvailabilityWhenCapacityIsNegative() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(),
                -1
        );

        assertThat(result.getSlots()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Capacity = 1
    // -------------------------------------------------------------------------

    @Test
    void shouldRemoveBookingFromAvailabilityWhenCapacityIsOne() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking =
                booking(
                        MONDAY.atTime(10, 0),
                        MONDAY.atTime(12, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(10, 0)
                        ),
                        slot(
                                MONDAY.atTime(12, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    @Test
    void shouldReturnNoAvailabilityWhenSingleBookingCoversEntireRule() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking =
                booking(
                        MONDAY.atTime(9, 0),
                        MONDAY.atTime(17, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking),
                1
        );

        assertThat(result.getSlots()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Capacity > 1
    // -------------------------------------------------------------------------

    @Test
    void shouldRemainAvailableWhenBookingsDoNotReachCapacity() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking1 =
                booking(
                        MONDAY.atTime(10, 0),
                        MONDAY.atTime(12, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking1),
                2
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    @Test
    void shouldRemainAvailableWhenBookingsEqualCapacity() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking1 =
                booking(
                        MONDAY.atTime(10, 0),
                        MONDAY.atTime(12, 0)
                );

        Booking booking2 =
                booking(
                        MONDAY.atTime(10, 0),
                        MONDAY.atTime(12, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking1, booking2),
                2
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(10, 0)
                        ),
                        slot(
                                MONDAY.atTime(12, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    @Test
    void shouldRemoveOnlyOverCapacityPeriod() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking1 =
                booking(
                        MONDAY.atTime(10, 0),
                        MONDAY.atTime(14, 0)
                );

        Booking booking2 =
                booking(
                        MONDAY.atTime(11, 0),
                        MONDAY.atTime(13, 0)
                );

        /*
         * Capacity = 1
         *
         * 09-10 -> 0 bookings -> available
         * 10-11 -> 1 booking  -> unavailable
         * 11-13 -> 2 bookings  -> unavailable
         * 13-14 -> 1 booking  -> unavailable
         * 14-17 -> 0 bookings  -> available
         */
        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking1, booking2),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(10, 0)
                        ),
                        slot(
                                MONDAY.atTime(14, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    @Test
    void shouldRemoveOnlyPeriodWhereCapacityIsExceeded() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking1 =
                booking(
                        MONDAY.atTime(10, 0),
                        MONDAY.atTime(14, 0)
                );

        Booking booking2 =
                booking(
                        MONDAY.atTime(11, 0),
                        MONDAY.atTime(13, 0)
                );

        Booking booking3 =
                booking(
                        MONDAY.atTime(12, 0),
                        MONDAY.atTime(15, 0)
                );

        /*
         * Capacity = 3
         *
         * 09-10 -> 0
         * 10-11 -> 1
         * 11-12 -> 2
         * 12-13 -> 3  <-- unavailable
         * 13-14 -> 2
         * 14-15 -> 1
         * 15-17 -> 0
         */
        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking1, booking2, booking3),
                3
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(12, 0)
                        ),
                        slot(
                                MONDAY.atTime(13, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    // -------------------------------------------------------------------------
    // Booking boundaries
    // -------------------------------------------------------------------------

    @Test
    void bookingEndingWhenAnotherBookingStartsShouldNotExceedCapacity() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking1 =
                booking(
                        MONDAY.atTime(10, 0),
                        MONDAY.atTime(12, 0)
                );

        Booking booking2 =
                booking(
                        MONDAY.atTime(12, 0),
                        MONDAY.atTime(14, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking1, booking2),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(10, 0)
                        ),
                        slot(
                                MONDAY.atTime(14, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    @Test
    void bookingOutsideAvailabilityShouldNotAffectAvailability() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking =
                booking(
                        MONDAY.atTime(18, 0),
                        MONDAY.atTime(20, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    // -------------------------------------------------------------------------
    // Availability rules
    // -------------------------------------------------------------------------

    @Test
    void shouldIgnoreRuleForDifferentDayOfWeek() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.TUESDAY, "09:00", "17:00");

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(),
                1
        );

        assertThat(result.getSlots()).isEmpty();
    }

    @Test
    void shouldMergeOverlappingAvailabilityRules() {

        ResourceAvailabilityRule rule1 =
                rule(DayOfWeek.MONDAY, "09:00", "13:00");

        ResourceAvailabilityRule rule2 =
                rule(DayOfWeek.MONDAY, "12:00", "17:00");

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule1, rule2),
                List.of(),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(17, 0)
                        )
                );
    }

    @Test
    void shouldRespectRuleEffectiveFromDate() {

        ResourceAvailabilityRule rule =
                rule(
                        DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"
                );

        rule.setEffectiveFrom(
                MONDAY.plusDays(7)
        );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(),
                1
        );

        assertThat(result.getSlots()).isEmpty();
    }

    @Test
    void shouldRespectRuleEffectiveToDate() {

        ResourceAvailabilityRule rule =
                rule(
                        DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"
                );

        rule.setEffectiveTo(
                MONDAY.minusDays(1)
        );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(),
                1
        );

        assertThat(result.getSlots()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Multiple days
    // -------------------------------------------------------------------------

    @Test
    void shouldCalculateAvailabilityAcrossMultipleDays() {

        LocalDate tuesday = MONDAY.plusDays(1);

        ResourceAvailabilityRule mondayRule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        ResourceAvailabilityRule tuesdayRule =
                rule(DayOfWeek.TUESDAY, "10:00", "16:00");

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                tuesday,
                List.of(mondayRule, tuesdayRule),
                List.of(),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                MONDAY.atTime(9, 0),
                                MONDAY.atTime(17, 0)
                        ),
                        slot(
                                tuesday.atTime(10, 0),
                                tuesday.atTime(16, 0)
                        )
                );
    }

    // -------------------------------------------------------------------------
    // Midnight crossing
    // -------------------------------------------------------------------------

    @Test
    void shouldAccountForBookingCrossingMidnight() {

        LocalDate tuesday = MONDAY.plusDays(1);

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.TUESDAY, "00:00", "08:00");

        Booking booking =
                booking(
                        MONDAY.atTime(23, 0),
                        tuesday.atTime(2, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                tuesday,
                tuesday,
                List.of(rule),
                List.of(booking),
                1
        );

        assertThat(result.getSlots())
                .containsExactly(
                        slot(
                                tuesday.atTime(2, 0),
                                tuesday.atTime(8, 0)
                        )
                );
    }

    @Test
    void shouldReturnNoAvailabilityWhenBookingsBlockEntireDay() {

        ResourceAvailabilityRule rule =
                rule(DayOfWeek.MONDAY, "09:00", "17:00");

        Booking booking1 =
                booking(
                        MONDAY.atTime(9, 0),
                        MONDAY.atTime(12, 0)
                );

        Booking booking2 =
                booking(
                        MONDAY.atTime(12, 0),
                        MONDAY.atTime(17, 0)
                );

        AvailabilityResponse result = calculator.calculate(
                MONDAY,
                MONDAY,
                List.of(rule),
                List.of(booking1, booking2),
                1
        );

        assertThat(result.getSlots()).isEmpty();
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ResourceAvailabilityRule rule(
            DayOfWeek dayOfWeek,
            String start,
            String end
    ) {

        ResourceAvailabilityRule rule =
                new ResourceAvailabilityRule();

        rule.setDayOfWeek(dayOfWeek);
        rule.setStartTime(LocalTime.parse(start));
        rule.setEndTime(LocalTime.parse(end));

        return rule;
    }

    private Booking booking(
            LocalDateTime start,
            LocalDateTime end
    ) {

        Booking booking = new Booking();

        /*
         * Adjust these setters if your Booking entity uses
         * different property names/types.
         */
        booking.setStartTime(
                start.atOffset(ZoneOffset.UTC)
        );

        booking.setEndTime(
                end.atOffset(ZoneOffset.UTC)
        );

        return booking;
    }

    private AvailabilitySlot slot(
            LocalDateTime start,
            LocalDateTime end
    ) {

        return AvailabilitySlot.builder()
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC))
                .build();
    }
}
