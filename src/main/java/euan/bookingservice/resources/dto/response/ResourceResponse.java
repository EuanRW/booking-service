package euan.bookingservice.resources.dto.response;

import euan.bookingservice.resources.entity.ResourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceResponse {
    private Long id;
    private String title;
    private String description;
    private Long ownerId;
    private LocalDateTime scheduledTime;
    private ResourceType resourceType;
}
