package euan.bookingservice.bookings.dto.response;

import euan.bookingservice.bookings.entity.BookingStatus;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BookingResponse {
    private Long id;
    private Long resourceId;
    private Long userId;
    private BookingStatus status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
