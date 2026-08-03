package euan.bookingservice.resources.dto.request;

import euan.bookingservice.resources.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Organizer is required")
    private Long organizerId;

    private LocalDateTime scheduledTime;

    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;
}
