package euan.lessonbookingservice.controller;

import euan.lessonbookingservice.dto.BookingDto;
import euan.lessonbookingservice.service.BookingService;
import euan.lessonbookingservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@RequestBody BookingDto bookingDto) {
        Long currentUserId = getCurrentUserId();

        // Ensure students can only create bookings for themselves
        if (!isAdmin() && !bookingDto.getStudentId().equals(currentUserId)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        BookingDto createdBooking = bookingService.createBooking(bookingDto);
        return ResponseEntity.ok(createdBooking);
    }

    @GetMapping
    public ResponseEntity<List<BookingDto>> getAllBookings() {
        Long currentUserId = getCurrentUserId();

        if (isAdmin()) {
            // Admins can see all bookings
            return ResponseEntity.ok(bookingService.getAllBookings());
        } else {
            // Students can only see their own bookings
            return ResponseEntity.ok(bookingService.getBookingsByStudentId(currentUserId));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDto> getBookingById(@PathVariable Long id) {
        Optional<BookingDto> booking = bookingService.getBookingById(id);

        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if user has permission to view this booking
        if (!hasPermissionToAccessBooking(booking.get())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        return ResponseEntity.ok(booking.get());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingDto> updateBooking(@PathVariable Long id, @RequestBody BookingDto bookingDto) {
        // First check if booking exists and user has permission
        Optional<BookingDto> existingBooking = bookingService.getBookingById(id);
        if (existingBooking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!hasPermissionToAccessBooking(existingBooking.get())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        // Ensure students can't change the studentId to someone else
        Long currentUserId = getCurrentUserId();
        if (!isAdmin() && !bookingDto.getStudentId().equals(currentUserId)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        Optional<BookingDto> updatedBooking = bookingService.updateBooking(id, bookingDto);
        return updatedBooking.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        // First check if booking exists and user has permission
        Optional<BookingDto> existingBooking = bookingService.getBookingById(id);
        if (existingBooking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!hasPermissionToAccessBooking(existingBooking.get())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        if (bookingService.deleteBooking(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getUserIdByUsername(username);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean hasPermissionToAccessBooking(BookingDto booking) {
        if (isAdmin()) {
            return true; // Admins can access all bookings
        }

        Long currentUserId = getCurrentUserId();
        return booking.getStudentId().equals(currentUserId);
    }
}