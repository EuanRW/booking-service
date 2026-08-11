package euan.bookingservice.bookings.port;

public interface ResourceLookup {

    boolean existsById(Long resourceId);
}