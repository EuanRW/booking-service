package euan.bookingservice.resources.entity;

import euan.bookingservice.common.audit.Auditable;
import euan.bookingservice.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "resources")
public class Resource extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    private User owner;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;
}
