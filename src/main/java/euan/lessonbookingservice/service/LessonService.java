package euan.lessonbookingservice.service;

import euan.lessonbookingservice.dto.LessonDto;
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

    public LessonDto createLesson(LessonDto lessonDto) {
        Lesson lesson = convertToEntity(lessonDto);
        Lesson savedLesson = lessonRepository.save(lesson);
        return convertToDto(savedLesson);
    }

    public List<LessonDto> getAllLessons() {
        return lessonRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<LessonDto> getLessonById(Long id) {
        return lessonRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<LessonDto> updateLesson(Long id, LessonDto lessonDto) {
        return lessonRepository.findById(id).map(existingLesson -> {
            existingLesson.setTitle(lessonDto.getTitle());
            existingLesson.setDescription(lessonDto.getDescription());

            Optional<User> teacherOpt = userRepository.findById(lessonDto.getTeacherId());
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

    private LessonDto convertToDto(Lesson lesson) {
        LessonDto dto = new LessonDto();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setDescription(lesson.getDescription());
        dto.setTeacherId(lesson.getTeacher().getId());
        return dto;
    }

    private Lesson convertToEntity(LessonDto dto) {
        Lesson lesson = new Lesson();
        lesson.setId(dto.getId());
        lesson.setTitle(dto.getTitle());
        lesson.setDescription(dto.getDescription());

        Optional<User> teacherOpt = userRepository.findById(dto.getTeacherId());
        if (teacherOpt.isPresent()) {
            lesson.setTeacher(teacherOpt.get());
        } else {
            throw new IllegalArgumentException("Teacher with ID " + dto.getTeacherId() + " not found.");
        }

        return lesson;
    }
}
