package euan.bookingservice.seed;

import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.bookings.entity.BookingStatus;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.entity.ResourceAvailabilityRule;
import euan.bookingservice.resources.entity.ResourceType;
import euan.bookingservice.users.entity.User;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Component
public class DemoDataFactory {

    public User admin() {
        User user = new User();

        user.setUsername("admin");

        // Encode before saving
        user.setPassword("password");

        user.setRole("ADMIN");

        return user;
    }

    public User user(String userName) {
        User user = new User();

        user.setUsername(userName);

        // Encode before saving
        user.setPassword("password");

        user.setRole("USER");

        return user;
    }

    public Resource event(String title, User owner) {
        Resource resource = new Resource();

        resource.setResourceType(ResourceType.EVENT);
        resource.setTitle(title);
        resource.setOwnerId(owner.getId());

        return resource;
    }

    public ResourceAvailabilityRule availabilityRule(Resource event) {
        ResourceAvailabilityRule availabilityRule = new ResourceAvailabilityRule();

        availabilityRule.setResource(event);
        availabilityRule.setDayOfWeek(DayOfWeek.FRIDAY);
        availabilityRule.setStartTime(LocalTime.NOON);
        availabilityRule.setEndTime(LocalTime.NOON.plusHours(1));

        return availabilityRule;
    }

    public Booking booking(User user, Resource resource) {

        Booking booking = new Booking();

        booking.setUser(user);
        booking.setResourceId(resource.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStartTime(OffsetDateTime.now());
        booking.setEndTime(OffsetDateTime.now().plusHours(1));

        return booking;
    }

}
