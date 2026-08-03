package euan.lessonbookingservice.dto.response;

import euan.lessonbookingservice.entity.ResourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceResponse {
    private Long id;
    private String title;
    private String description;
    private Long organizerId;
    private LocalDateTime scheduledTime;
    private ResourceType resourceType;
}
