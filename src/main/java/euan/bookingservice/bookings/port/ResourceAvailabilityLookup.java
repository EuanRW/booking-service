package euan.bookingservice.bookings.port;

import java.time.OffsetDateTime;

public interface ResourceAvailabilityLookup {
    boolean isAvailable(
            Long resourceId,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    );

    boolean isAvailableExcludingBooking(
            Long resourceId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Long bookingId
    );
}