package euan.lessonbookingservice.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LessonResponse {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private LocalDateTime scheduledTime;
}
