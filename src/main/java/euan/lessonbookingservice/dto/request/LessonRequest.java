package euan.lessonbookingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LessonRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @NotBlank(message = "Teacher is required")
    private Long teacherId;
    @NotBlank(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;
}
