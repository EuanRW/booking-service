package euan.bookingservice.service;

import euan.bookingservice.bookings.dto.request.BookingRequest;
import euan.bookingservice.bookings.dto.response.BookingResponse;
import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.bookings.service.BookingService;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBookingMapsResourceIdToBooking() {
        Resource resource = new Resource();
        resource.setId(10L);

        User user = new User();
        user.setId(4L);

        BookingRequest request = new BookingRequest();
        request.setResourceId(10L);
        request.setUserId(4L);

        Booking savedBooking = new Booking();
        savedBooking.setId(99L);
        savedBooking.setResource(resource);
        savedBooking.setUser(user);

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request);

        assertEquals(99L, response.getId());
        assertEquals(10L, response.getResourceId());
        assertEquals(4L, response.getUserId());
    }
}
