package euan.bookingservice.resources.port;

public interface BookingLookup {
    boolean existsByResourceId(Long resourceId);
}
