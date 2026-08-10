package euan.bookingservice.resources.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class AvailabilitySlot {
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}