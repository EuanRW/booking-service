package euan.bookingservice.bookings.exception;

public class BookingOutsideAvailabilityException extends InvalidBookingException {

    public BookingOutsideAvailabilityException(String message) {
        super(message);
    }
}