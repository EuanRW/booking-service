package euan.bookingservice.resources.adapter;

import euan.bookingservice.bookings.port.ResourceAvailabilityLookup;
import euan.bookingservice.resources.service.AvailabilityService;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class BookingResourceAvailabilityLookupAdapter implements ResourceAvailabilityLookup {

    private final AvailabilityService availabilityService;

    public BookingResourceAvailabilityLookupAdapter(
            AvailabilityService availabilityService
    ) {
        this.availabilityService = availabilityService;
    }

    @Override
    public boolean isAvailable(
            Long resourceId,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) {
        return availabilityService.isAvailable(
                resourceId,
                startTime,
                endTime
        );
    }

    @Override
    public boolean isAvailableExcludingBooking(
            Long resourceId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Long bookingId
    ) {
        return availabilityService.isAvailableExcludingBooking(
                resourceId,
                startTime,
                endTime,
                bookingId
        );
    }
}