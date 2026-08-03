package euan.lessonbookingservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {
    private Long resourceId;
    private Long lessonId;

    @NotNull(message = "Student ID is required")
    private Long studentId;
}
