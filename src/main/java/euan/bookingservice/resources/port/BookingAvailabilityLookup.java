package euan.bookingservice.resources.port;

import euan.bookingservice.resources.model.OccupiedInterval;

import java.time.OffsetDateTime;
import java.util.List;

public interface BookingAvailabilityLookup {

    List<OccupiedInterval> findOccupiedIntervals(
            Long resourceId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}