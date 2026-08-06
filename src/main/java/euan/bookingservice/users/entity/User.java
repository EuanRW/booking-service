package euan.bookingservice.users.entity;

import euan.bookingservice.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String role; // USER, ORGANIZER, ADMIN

    @Version
    private Integer version;
}