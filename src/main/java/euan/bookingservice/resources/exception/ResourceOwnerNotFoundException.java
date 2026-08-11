package euan.bookingservice.resources.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceOwnerNotFoundException extends RuntimeException {
    public ResourceOwnerNotFoundException(String message) {
        super(message);
    }
}