package euan.bookingservice.resources.exception;

public class InvalidAvailabilityRuleException extends RuntimeException {

    public InvalidAvailabilityRuleException(String message) {
        super(message);
    }
}