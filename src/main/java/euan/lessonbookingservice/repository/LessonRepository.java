package euan.lessonbookingservice.repository;

import euan.lessonbookingservice.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {}
