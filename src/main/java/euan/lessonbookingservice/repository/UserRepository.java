package euan.lessonbookingservice.repository;

import euan.lessonbookingservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
