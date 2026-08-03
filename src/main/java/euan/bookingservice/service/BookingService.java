package euan.bookingservice.service;

import euan.bookingservice.dto.request.BookingRequest;
import euan.bookingservice.dto.response.BookingResponse;
import euan.bookingservice.entity.Booking;
import euan.bookingservice.entity.Resource;
import euan.bookingservice.entity.User;
import euan.bookingservice.repository.BookingRepository;
import euan.bookingservice.repository.ResourceRepository;
import euan.bookingservice.repository.UserRepository;
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
        Booking savedBooking = bookingRepository.save(booking);
        return convertToDto(savedBooking);
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByStudentId(Long studentId) {
        return bookingRepository.findByStudentId(studentId)
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

            Optional<User> studentOpt = userRepository.findById(bookingRequest.getStudentId());
            studentOpt.ifPresent(existingBooking::setStudent);

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
        dto.setStudentId(booking.getStudent() != null ? booking.getStudent().getId() : null);
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

        Optional<User> studentOpt = userRepository.findById(bookingRequest.getStudentId());
        if (studentOpt.isPresent()) {
            booking.setStudent(studentOpt.get());
        } else {
            throw new IllegalArgumentException("Student with ID " + bookingRequest.getStudentId() + " not found.");
        }

        return booking;
    }
}