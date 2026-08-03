package euan.lessonbookingservice.service;

import euan.lessonbookingservice.dto.request.BookingRequest;
import euan.lessonbookingservice.dto.response.BookingResponse;
import euan.lessonbookingservice.entity.Booking;
import euan.lessonbookingservice.entity.Resource;
import euan.lessonbookingservice.entity.User;
import euan.lessonbookingservice.repository.BookingRepository;
import euan.lessonbookingservice.repository.ResourceRepository;
import euan.lessonbookingservice.repository.UserRepository;
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

        User student = new User();
        student.setId(4L);

        BookingRequest request = new BookingRequest();
        request.setResourceId(10L);
        request.setStudentId(4L);

        Booking savedBooking = new Booking();
        savedBooking.setId(99L);
        savedBooking.setResource(resource);
        savedBooking.setStudent(student);

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(4L)).thenReturn(Optional.of(student));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request);

        assertEquals(99L, response.getId());
        assertEquals(10L, response.getResourceId());
        assertEquals(4L, response.getStudentId());
    }
}
