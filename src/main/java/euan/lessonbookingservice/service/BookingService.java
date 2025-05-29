package euan.lessonbookingservice.service;

import euan.lessonbookingservice.dto.BookingDto;
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

    public BookingDto createBooking(BookingDto bookingDto) {
        Booking booking = convertToEntity(bookingDto);
        Booking savedBooking = bookingRepository.save(booking);
        return convertToDto(savedBooking);
    }

    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // New method to get bookings by student ID
    public List<BookingDto> getBookingsByStudentId(Long studentId) {
        return bookingRepository.findByStudentId(studentId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<BookingDto> getBookingById(Long id) {
        return bookingRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<BookingDto> updateBooking(Long id, BookingDto bookingDto) {
        return bookingRepository.findById(id).map(existingBooking -> {
            Optional<Lesson> lessonOpt = lessonRepository.findById(bookingDto.getLessonId());
            lessonOpt.ifPresent(existingBooking::setLesson);

            Optional<User> studentOpt = userRepository.findById(bookingDto.getStudentId());
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

    private BookingDto convertToDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setLessonId(booking.getLesson().getId());
        dto.setStudentId(booking.getStudent().getId());
        return dto;
    }

    private Booking convertToEntity(BookingDto dto) {
        Booking booking = new Booking();
        booking.setId(dto.getId());

        Optional<Lesson> lessonOpt = lessonRepository.findById(dto.getLessonId());
        if (lessonOpt.isPresent()) {
            booking.setLesson(lessonOpt.get());
        } else {
            throw new IllegalArgumentException("Lesson with ID " + dto.getLessonId() + " not found.");
        }

        Optional<User> studentOpt = userRepository.findById(dto.getStudentId());
        if (studentOpt.isPresent()) {
            booking.setStudent(studentOpt.get());
        } else {
            throw new IllegalArgumentException("Student with ID " + dto.getStudentId() + " not found.");
        }

        return booking;
    }
}