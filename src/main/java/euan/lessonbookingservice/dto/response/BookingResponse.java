package euan.lessonbookingservice.dto.response;

import lombok.Data;

@Data
public class BookingResponse {
    private Long id;
    private Long resourceId;
    private Long studentId;
}
