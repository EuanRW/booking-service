package euan.lessonbookingservice.controller;

import euan.lessonbookingservice.dto.request.BookingRequest;
import euan.lessonbookingservice.dto.response.BookingResponse;
import euan.lessonbookingservice.service.BookingService;
import euan.lessonbookingservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bookings")
@Tag(name = "Bookings", description = "Booking management API")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new booking",
            description = "Students can only create bookings for themselves. Admins can create bookings for any student."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot create booking for another student",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content)
    })
    public ResponseEntity<BookingResponse> createBooking(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Booking details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookingRequest.class))
            )
            @Valid @RequestBody BookingRequest bookingRequest) {
        Long currentUserId = getCurrentUserId();

        // Ensure students can only create bookings for themselves
        if (!isAdmin() && !bookingRequest.getStudentId().equals(currentUserId)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        BookingResponse createdBooking = bookingService.createBooking(bookingRequest);
        return ResponseEntity.status(201).body(createdBooking);
    }

    @GetMapping
    @Operation(
            summary = "Get all bookings",
            description = "Students see only their own bookings. Admins see all bookings."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bookings",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class)))
    })
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
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
    @Operation(
            summary = "Get booking by ID",
            description = "Students can only view their own bookings. Admins can view any booking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access this booking",
                    content = @Content)
    })
    public ResponseEntity<BookingResponse> getBookingById(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long id) {
        Optional<BookingResponse> booking = bookingService.getBookingById(id);

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
    @Operation(
            summary = "Update booking",
            description = "Students can only update their own bookings. Admins can update any booking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot update this booking",
                    content = @Content)
    })
    public ResponseEntity<BookingResponse> updateBooking(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated booking details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookingRequest.class))
            )
            @Valid @RequestBody BookingRequest bookingRequest) {
        // First check if booking exists and user has permission
        Optional<BookingResponse> existingBooking = bookingService.getBookingById(id);
        if (existingBooking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!hasPermissionToAccessBooking(existingBooking.get())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        // Ensure students can't change the studentId to someone else
        Long currentUserId = getCurrentUserId();
        if (!isAdmin() && !bookingRequest.getStudentId().equals(currentUserId)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        Optional<BookingResponse> updatedBooking = bookingService.updateBooking(id, bookingRequest);
        return updatedBooking.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete booking",
            description = "Students can only delete their own bookings. Admins can delete any booking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Booking deleted successfully",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot delete this booking",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteBooking(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long id) {
        // First check if booking exists and user has permission
        Optional<BookingResponse> existingBooking = bookingService.getBookingById(id);
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

    private boolean hasPermissionToAccessBooking(BookingResponse booking) {
        if (isAdmin()) {
            return true; // Admins can access all bookings
        }

        Long currentUserId = getCurrentUserId();
        return booking.getStudentId().equals(currentUserId);
    }
}