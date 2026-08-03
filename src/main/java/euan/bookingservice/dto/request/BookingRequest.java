package euan.bookingservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {
    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Student ID is required")
    private Long studentId;
}
