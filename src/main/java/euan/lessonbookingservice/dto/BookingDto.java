package euan.lessonbookingservice.dto;

import lombok.Data;

@Data
public class BookingDto {
    private Long id;
    private Long lessonId;
    private Long studentId;
}
