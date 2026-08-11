package euan.bookingservice.resources.model;

import java.time.OffsetDateTime;

public record OccupiedInterval(
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}