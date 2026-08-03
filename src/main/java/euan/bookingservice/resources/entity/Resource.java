package euan.bookingservice.resources.entity;

import euan.bookingservice.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    private User organizer;

    private LocalDateTime scheduledTime;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;
}
