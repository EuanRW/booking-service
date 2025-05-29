package euan.lessonbookingservice.service;

import euan.lessonbookingservice.dto.request.BookingRequest;
import euan.lessonbookingservice.dto.response.BookingResponse;
import euan.lessonbookingservice.entity.Booking;
import euan.lessonbookingservice.entity.Lesson;
import euan.lessonbookingservice.entity.User;
import euan.lessonbookingservice.repository.BookingRepository;
import euan.lessonbookingservice.repository.LessonRepository;
import euan.lessonbookingservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, LessonRepository lessonRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.lessonRepository = lessonRepository;
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

    // New method to get bookings by student ID
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
            Optional<Lesson> lessonOpt = lessonRepository.findById(bookingRequest.getLessonId());
            lessonOpt.ifPresent(existingBooking::setLesson);

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
        dto.setLessonId(booking.getLesson().getId());
        dto.setStudentId(booking.getStudent().getId());
        return dto;
    }

    private Booking convertToEntity(BookingRequest bookingRequest) {
        Booking booking = new Booking();

        Optional<Lesson> lessonOpt = lessonRepository.findById(bookingRequest.getLessonId());
        if (lessonOpt.isPresent()) {
            booking.setLesson(lessonOpt.get());
        } else {
            throw new IllegalArgumentException("Lesson with ID " + bookingRequest.getLessonId() + " not found.");
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