package euan.bookingservice.bookings.adapter;

import euan.bookingservice.bookings.entity.BookingStatus;
import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.resources.model.OccupiedInterval;
import euan.bookingservice.resources.port.BookingAvailabilityLookup;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class BookingAvailabilityAdapter implements BookingAvailabilityLookup {

    private final BookingRepository bookingRepository;

    public BookingAvailabilityAdapter(
            BookingRepository bookingRepository
    ) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<OccupiedInterval> findOccupiedIntervals(
            Long resourceId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return bookingRepository
                .findByResourceIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        resourceId,
                        BookingStatus.CONFIRMED,
                        to,
                        from
                )
                .stream()
                .map(booking -> new OccupiedInterval(
                        booking.getStartTime(),
                        booking.getEndTime()
                ))
                .toList();
    }

    @Override
    public List<OccupiedInterval> findOccupiedIntervalsExcludingBooking(
            Long resourceId,
            OffsetDateTime from,
            OffsetDateTime to,
            Long bookingId
    ) {
        return bookingRepository
                .findByResourceIdAndStatusAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        resourceId,
                        BookingStatus.CONFIRMED,
                        bookingId,
                        to,
                        from
                )
                .stream()
                .map(booking -> new OccupiedInterval(
                        booking.getStartTime(),
                        booking.getEndTime()
                ))
                .toList();
    }
}