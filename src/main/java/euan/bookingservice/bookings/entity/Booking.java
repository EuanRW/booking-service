package euan.bookingservice.bookings.entity;

import euan.bookingservice.common.audit.Auditable;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "bookings")
public class Booking extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Resource resource;

    @ManyToOne
    private User user;
}