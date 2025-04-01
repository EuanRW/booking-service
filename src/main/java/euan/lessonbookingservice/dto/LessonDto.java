package euan.lessonbookingservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LessonDto {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
}
