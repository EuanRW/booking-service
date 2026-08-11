package euan.bookingservice.resources.entity;

import euan.bookingservice.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "owner_id")
    private Long ownerId;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResourceAvailabilityRule> availabilityRules = new ArrayList<>();
}
