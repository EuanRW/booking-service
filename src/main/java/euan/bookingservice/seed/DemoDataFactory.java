package euan.bookingservice.seed;

import euan.bookingservice.bookings.entity.Booking;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.entity.ResourceType;
import euan.bookingservice.users.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DemoDataFactory {

    public User admin() {
        User user = new User();

        user.setUsername("Admin");

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

    public Resource event(String title, User organizer) {
        Resource resource = new Resource();

        resource.setResourceType(ResourceType.EVENT);
        resource.setTitle(title);
        resource.setOrganizer(organizer);

        return resource;
    }

    public Booking booking(User user, Resource resource) {

        Booking booking = new Booking();

        booking.setUser(user);
        booking.setResource(resource);

        return booking;
    }

}
