package euan.lessonbookingservice.repository;

import euan.lessonbookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find all bookings for a specific student
    List<Booking> findByStudentId(Long studentId);

    // Optional: Find all bookings for a specific lesson
    List<Booking> findByLessonId(Long lessonId);
}