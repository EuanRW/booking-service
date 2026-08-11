package euan.bookingservice.resources.service;

import euan.bookingservice.resources.model.OccupiedInterval;
import euan.bookingservice.resources.dto.response.AvailabilityResponse;
import euan.bookingservice.resources.dto.response.AvailabilitySlot;
import euan.bookingservice.resources.entity.ResourceAvailabilityRule;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class AvailabilityCalculator {

    public AvailabilityResponse calculate(
            LocalDate from,
            LocalDate to,
            List<ResourceAvailabilityRule> rules,
            List<OccupiedInterval> occupiedIntervals,
            Integer capacity
    ) {

        List<AvailabilitySlot> slots = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {

            LocalDate currentDate = date;

            List<Interval> ruleIntervals = rules.stream()
                    .filter(rule ->
                            rule.getDayOfWeek() == currentDate.getDayOfWeek())
                    .filter(rule ->
                            isEffective(rule, currentDate))
                    .map(rule -> new Interval(
                            currentDate.atTime(rule.getStartTime()),
                            currentDate.atTime(rule.getEndTime())
                    ))
                    .toList();

            List<Interval> mergedRules = merge(ruleIntervals);

            List<Interval> bookingIntervals = occupiedIntervals.stream()
                    .map(interval -> new Interval(
                            interval.startTime().toLocalDateTime(),
                            interval.endTime().toLocalDateTime()
                    ))
                    .filter(interval ->
                            overlapsDay(interval, currentDate))
                    .sorted(Comparator.comparing(interval -> interval.start))
                    .toList();

            List<Interval> available;

            if (capacity == null) {
                /*
                 * Null capacity means unlimited capacity.
                 *
                 * Bookings therefore do not reduce availability.
                 */
                available = mergedRules;

            } else if (capacity <= 0) {
                /*
                 * A configured capacity of zero or less means
                 * the resource cannot be booked.
                 */
                available = List.of();

            } else {
                /*
                 * Capacity is configured, so only periods where
                 * concurrent bookings are below capacity are available.
                 */
                available = subtract(
                        mergedRules,
                        bookingIntervals,
                        capacity
                );
            }

            for (Interval interval : available) {

                if (interval.start.isBefore(interval.end)) {
                    slots.add(
                            AvailabilitySlot.builder()
                                    .startTime(
                                            interval.start.atOffset(
                                                    ZoneOffset.UTC
                                            )
                                    )
                                    .endTime(
                                            interval.end.atOffset(
                                                    ZoneOffset.UTC
                                            )
                                    )
                                    .build()
                    );
                }
            }
        }

        return AvailabilityResponse.builder()
                .slots(slots)
                .build();
    }

    private boolean isEffective(
            ResourceAvailabilityRule rule,
            LocalDate date
    ) {

        if (rule.getEffectiveFrom() != null
                && date.isBefore(rule.getEffectiveFrom())) {
            return false;
        }

        if (rule.getEffectiveTo() != null
                && date.isAfter(rule.getEffectiveTo())) {
            return false;
        }

        return true;
    }

    private List<Interval> merge(
            List<Interval> intervals
    ) {

        if (intervals.isEmpty()) {
            return List.of();
        }

        List<Interval> sorted = intervals.stream()
                .sorted(Comparator.comparing(i -> i.start))
                .toList();

        List<Interval> merged = new ArrayList<>();

        Interval current = sorted.getFirst();

        for (int i = 1; i < sorted.size(); i++) {

            Interval next = sorted.get(i);

            if (!next.start.isAfter(current.end)) {

                if (next.end.isAfter(current.end)) {
                    current = new Interval(
                            current.start,
                            next.end
                    );
                }

            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);

        return merged;
    }

    private List<Interval> subtract(
            List<Interval> availability,
            List<Interval> bookings,
            int capacity
    ) {

        List<Interval> result = new ArrayList<>();

        for (Interval available : availability) {

            List<Event> events = new ArrayList<>();

            for (Interval booking : bookings) {

                if (!overlaps(available, booking)) {
                    continue;
                }

                LocalDateTime start = max(
                        available.start,
                        booking.start
                );

                LocalDateTime end = min(
                        available.end,
                        booking.end
                );

                if (start.isBefore(end)) {
                    events.add(new Event(start, +1));
                    events.add(new Event(end, -1));
                }
            }

            if (events.isEmpty()) {
                result.add(available);
                continue;
            }

            events.sort(
                    Comparator
                            .comparing(Event::time)
                            .thenComparing(Event::delta)
            );

            LocalDateTime cursor = available.start;
            int activeBookings = 0;
            int index = 0;

            while (index < events.size()) {

                LocalDateTime eventTime =
                        events.get(index).time;

                if (cursor.isBefore(eventTime)
                        && activeBookings < capacity) {

                    result.add(
                            new Interval(
                                    cursor,
                                    eventTime
                            )
                    );
                }

                while (index < events.size()
                        && events.get(index).time.equals(eventTime)) {

                    activeBookings += events.get(index).delta;
                    index++;
                }

                cursor = eventTime;
            }

            if (cursor.isBefore(available.end)
                    && activeBookings < capacity) {

                result.add(
                        new Interval(
                                cursor,
                                available.end
                        )
                );
            }
        }

        // Combine adjacent availability created by booking boundaries.
        return merge(result);
    }

    private boolean overlapsDay(
            Interval interval,
            LocalDate date
    ) {

        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1).atStartOfDay();

        return interval.start.isBefore(dayEnd)
                && interval.end.isAfter(dayStart);
    }

    private boolean overlaps(
            Interval a,
            Interval b
    ) {

        return a.start.isBefore(b.end)
                && b.start.isBefore(a.end);
    }

    private LocalDateTime max(
            LocalDateTime a,
            LocalDateTime b
    ) {

        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(
            LocalDateTime a,
            LocalDateTime b
    ) {

        return a.isBefore(b) ? a : b;
    }

    private record Interval(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }

    private record Event(
            LocalDateTime time,
            int delta
    ) {
    }
}
