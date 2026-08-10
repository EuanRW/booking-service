package euan.bookingservice.resources.service;

import euan.bookingservice.bookings.entity.Booking;
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
            List<Booking> bookings
    ) {

        List<AvailabilitySlot> slots = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {

            LocalDate currentDate = date;

            List<Interval> ruleIntervals = rules.stream()
                    .filter(rule -> rule.getDayOfWeek() == currentDate.getDayOfWeek())
                    .filter(rule -> isEffective(rule, currentDate))
                    .map(rule -> new Interval(
                            currentDate.atTime(rule.getStartTime()),
                            currentDate.atTime(rule.getEndTime())))
                    .toList();

            List<Interval> mergedRules = merge(ruleIntervals);

            List<Interval> bookingIntervals = bookings.stream()
                    .map(b -> new Interval(
                            b.getStartTime().toLocalDateTime(),
                            b.getEndTime().toLocalDateTime()))
                    .filter(i -> i.start.toLocalDate().equals(currentDate))
                    .sorted(Comparator.comparing(i -> i.start))
                    .toList();

            List<Interval> available = subtract(mergedRules, bookingIntervals);

            for (Interval interval : available) {
                slots.add(
                        AvailabilitySlot.builder()
                                .startTime(interval.start.atOffset(ZoneOffset.UTC))
                                .endTime(interval.end.atOffset(ZoneOffset.UTC))
                                .build()
                );
            }
        }
        return AvailabilityResponse.builder()
                .slots(slots)
                .build();
    }

    private boolean isEffective(ResourceAvailabilityRule rule, LocalDate date) {

        if (rule.getEffectiveFrom() != null &&
                date.isBefore(rule.getEffectiveFrom())) {
            return false;
        }

        if (rule.getEffectiveTo() != null &&
                date.isAfter(rule.getEffectiveTo())) {
            return false;
        }

        return true;
    }

    private List<Interval> merge(List<Interval> intervals) {

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
                    current = new Interval(current.start, next.end);
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
            List<Interval> bookings
    ) {

        List<Interval> remaining = new ArrayList<>(availability);

        for (Interval booking : bookings) {

            List<Interval> next = new ArrayList<>();

            for (Interval slot : remaining) {

                if (!overlaps(slot, booking)) {
                    next.add(slot);
                    continue;
                }

                if (booking.start.isAfter(slot.start)) {
                    next.add(new Interval(
                            slot.start,
                            booking.start
                    ));
                }

                if (booking.end.isBefore(slot.end)) {
                    next.add(new Interval(
                            booking.end,
                            slot.end
                    ));
                }
            }

            remaining = next;
        }

        return remaining;
    }

    private boolean overlaps(Interval a, Interval b) {
        return a.start.isBefore(b.end)
                && b.start.isBefore(a.end);
    }

    private record Interval(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }
}