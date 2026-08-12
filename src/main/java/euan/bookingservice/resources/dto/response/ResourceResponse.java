package euan.bookingservice.resources.dto.response;

import euan.bookingservice.resources.entity.ResourceType;
import lombok.Data;

@Data
public class ResourceResponse {
    private Long id;
    private String title;
    private String description;
    private Long ownerId;
    private Integer capacity;
    private ResourceType resourceType;
}
