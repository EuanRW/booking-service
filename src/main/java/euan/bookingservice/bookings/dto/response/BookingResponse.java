package euan.bookingservice.bookings.dto.response;

import lombok.Data;

@Data
public class BookingResponse {
    private Long id;
    private Long resourceId;
    private Long userId;
}
