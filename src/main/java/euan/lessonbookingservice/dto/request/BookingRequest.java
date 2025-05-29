package euan.lessonbookingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRequest {
    @NotBlank(message = "Lesson ID is required")
    private Long lessonId;
    @NotBlank(message = "Student ID is required")
    private Long studentId;
}
