package euan.bookingservice.resources.adapter;

import euan.bookingservice.bookings.port.ResourceLookup;
import euan.bookingservice.resources.repository.ResourceRepository;
import org.springframework.stereotype.Component;

@Component
public class BookingResourceLookupAdapter implements ResourceLookup {

    private final ResourceRepository resourceRepository;

    public BookingResourceLookupAdapter(
            ResourceRepository resourceRepository
    ) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    public boolean existsById(Long resourceId) {
        return resourceRepository.existsById(resourceId);
    }
}