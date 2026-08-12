package euan.bookingservice.bookings.repository;

import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.bookings.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByResourceIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long resourceId,
            BookingStatus status,
            OffsetDateTime end,
            OffsetDateTime start
    );

    List<Booking>
    findByResourceIdAndStatusAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long resourceId,
            BookingStatus status,
            Long bookingId,
            OffsetDateTime end,
            OffsetDateTime start
    );

    boolean existsByResourceId(Long resourceId);
}