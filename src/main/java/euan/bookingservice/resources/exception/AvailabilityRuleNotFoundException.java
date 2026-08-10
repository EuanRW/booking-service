package euan.bookingservice.resources.exception;

public class AvailabilityRuleNotFoundException extends RuntimeException {

    public AvailabilityRuleNotFoundException(String message) {
        super(message);
    }
}