package euan.bookingservice.bookings.service;

import euan.bookingservice.bookings.dto.request.BookingRequest;
import euan.bookingservice.bookings.dto.response.BookingResponse;
import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.bookings.entity.BookingStatus;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, ResourceRepository resourceRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public BookingResponse createBooking(BookingRequest bookingRequest) {
        Booking booking = convertToEntity(bookingRequest);
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);
        return convertToDto(savedBooking);
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<BookingResponse> getBookingById(Long id) {
        return bookingRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<BookingResponse> updateBooking(Long id, BookingRequest bookingRequest) {
        return bookingRepository.findById(id).map(existingBooking -> {
            Optional<Resource> resourceOpt = resourceRepository.findById(bookingRequest.getResourceId());
            resourceOpt.ifPresent(existingBooking::setResource);

            Optional<User> userOpt = userRepository.findById(bookingRequest.getUserId());
            userOpt.ifPresent(existingBooking::setUser);

            Booking updatedBooking = bookingRepository.save(existingBooking);
            return convertToDto(updatedBooking);
        });
    }


    public boolean deleteBooking(Long id) {
        if (bookingRepository.existsById(id)) {
            bookingRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private BookingResponse convertToDto(Booking booking) {
        BookingResponse dto = new BookingResponse();
        dto.setId(booking.getId());
        dto.setResourceId(booking.getResource() != null ? booking.getResource().getId() : null);
        dto.setUserId(booking.getUser() != null ? booking.getUser().getId() : null);
        return dto;
    }

    private Booking convertToEntity(BookingRequest bookingRequest) {
        Booking booking = new Booking();

        Optional<Resource> resourceOpt = resourceRepository.findById(bookingRequest.getResourceId());
        if (resourceOpt.isPresent()) {
            booking.setResource(resourceOpt.get());
        } else {
            throw new IllegalArgumentException("Resource with ID " + bookingRequest.getResourceId() + " not found.");
        }

        Optional<User> userOpt = userRepository.findById(bookingRequest.getUserId());
        if (userOpt.isPresent()) {
            booking.setUser(userOpt.get());
        } else {
            throw new IllegalArgumentException("User with ID " + bookingRequest.getUserId() + " not found.");
        }

        return booking;
    }
}