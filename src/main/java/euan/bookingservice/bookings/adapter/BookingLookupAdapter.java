package euan.bookingservice.bookings.adapter;

import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.resources.port.BookingLookup;
import org.springframework.stereotype.Component;

@Component
public class BookingLookupAdapter implements BookingLookup {

    private final BookingRepository bookingRepository;

    public BookingLookupAdapter(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public boolean existsByResourceId(Long resourceId) {
        return bookingRepository.existsByResourceId(resourceId);
    }
}