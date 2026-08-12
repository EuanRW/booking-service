package euan.bookingservice.resources.exception;

public class ResourceHasBookingsException extends RuntimeException {

    public ResourceHasBookingsException(String message) {
        super(message);
    }
}