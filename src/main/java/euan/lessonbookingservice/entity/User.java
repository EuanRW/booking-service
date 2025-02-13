package euan.lessonbookingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String role; // STUDENT, TEACHER

//    @OneToMany(mappedBy = "teacher")
//    private List<Lesson> lessons;
}