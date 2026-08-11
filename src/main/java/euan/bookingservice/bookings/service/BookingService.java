package euan.bookingservice.bookings.service;

import euan.bookingservice.bookings.dto.request.BookingRequest;
import euan.bookingservice.bookings.dto.response.BookingResponse;
import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.bookings.entity.BookingStatus;
import euan.bookingservice.bookings.port.ResourceLookup;
import euan.bookingservice.resources.exception.ResourceNotFoundException;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ResourceLookup resourceLookup;;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          ResourceLookup resourceLookup,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceLookup = resourceLookup;
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
            Long resourceId = bookingRequest.getResourceId();

            if (!resourceLookup.existsById(resourceId)) {
                throw new ResourceNotFoundException(
                        "Resource with ID " + resourceId + " not found."
                );
            }

            existingBooking.setResourceId(resourceId);

            Optional<User> userOpt = userRepository.findById(bookingRequest.getUserId());
            userOpt.ifPresent(existingBooking::setUser);

            existingBooking.setStartTime(bookingRequest.getStartTime());
            existingBooking.setEndTime(bookingRequest.getEndTime());

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
        dto.setResourceId(booking.getResourceId());
        dto.setUserId(booking.getUser() != null ? booking.getUser().getId() : null);
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        return dto;
    }

    private Booking convertToEntity(BookingRequest bookingRequest) {
        Booking booking = new Booking();

        Long resourceId = bookingRequest.getResourceId();

        if (!resourceLookup.existsById(resourceId)) {
            throw new ResourceNotFoundException(
                    "Resource with ID " + resourceId + " not found."
            );
        }

        booking.setResourceId(resourceId);

        Optional<User> userOpt = userRepository.findById(bookingRequest.getUserId());
        if (userOpt.isPresent()) {
            booking.setUser(userOpt.get());
        } else {
            throw new IllegalArgumentException("User with ID " + bookingRequest.getUserId() + " not found.");
        }

        booking.setStartTime(bookingRequest.getStartTime());
        booking.setEndTime(bookingRequest.getEndTime());

        return booking;
    }
}