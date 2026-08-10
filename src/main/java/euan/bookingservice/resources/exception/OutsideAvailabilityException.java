package euan.bookingservice.resources.exception;

public class OutsideAvailabilityException extends RuntimeException {

    public OutsideAvailabilityException(String message) {
        super(message);
    }
}