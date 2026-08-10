package euan.bookingservice.resources.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AvailabilityResponse {
    private List<AvailabilitySlot> slots;
}
