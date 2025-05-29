package euan.lessonbookingservice.service;

import euan.lessonbookingservice.dto.request.LessonRequest;
import euan.lessonbookingservice.dto.response.LessonResponse;
import euan.lessonbookingservice.entity.Lesson;
import euan.lessonbookingservice.entity.User;
import euan.lessonbookingservice.repository.LessonRepository;
import euan.lessonbookingservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LessonService {
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository; // Needed to fetch teacher by ID

    public LessonService(LessonRepository lessonRepository, UserRepository userRepository) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    public LessonResponse createLesson(LessonRequest lessonRequest) {
        Lesson lesson = convertToEntity(lessonRequest);
        Lesson savedLesson = lessonRepository.save(lesson);
        return convertToDto(savedLesson);
    }

    public List<LessonResponse> getAllLessons() {
        return lessonRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<LessonResponse> getLessonById(Long id) {
        return lessonRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<LessonResponse> updateLesson(Long id, LessonRequest lessonRequest) {
        return lessonRepository.findById(id).map(existingLesson -> {
            existingLesson.setTitle(lessonRequest.getTitle());
            existingLesson.setDescription(lessonRequest.getDescription());
            existingLesson.setScheduledTime(lessonRequest.getScheduledTime());

            Optional<User> teacherOpt = userRepository.findById(lessonRequest.getTeacherId());
            teacherOpt.ifPresent(existingLesson::setTeacher);

            Lesson updatedLesson = lessonRepository.save(existingLesson);
            return convertToDto(updatedLesson);
        });
    }

    public boolean deleteLesson(Long id) {
        if (lessonRepository.existsById(id)) {
            lessonRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private LessonResponse convertToDto(Lesson lesson) {
        LessonResponse dto = new LessonResponse();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setDescription(lesson.getDescription());
        dto.setTeacherId(lesson.getTeacher().getId());
        return dto;
    }

    private Lesson convertToEntity(LessonRequest lessonRequest) {
        Lesson lesson = new Lesson();
        lesson.setTitle(lessonRequest.getTitle());
        lesson.setDescription(lessonRequest.getDescription());
        lesson.setScheduledTime(lessonRequest.getScheduledTime());

        Optional<User> teacherOpt = userRepository.findById(lessonRequest.getTeacherId());
        if (teacherOpt.isPresent()) {
            lesson.setTeacher(teacherOpt.get());
        } else {
            throw new IllegalArgumentException("Teacher with ID " + lessonRequest.getTeacherId() + " not found.");
        }

        return lesson;
    }
}
