package euan.lessonbookingservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LessonDto {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private LocalDateTime scheduledTime;
}
