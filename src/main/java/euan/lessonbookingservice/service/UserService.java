package euan.lessonbookingservice.service;

import euan.lessonbookingservice.dto.request.UserUpdateRequest;
import euan.lessonbookingservice.dto.response.UserResponse;
import euan.lessonbookingservice.entity.User;
import euan.lessonbookingservice.exception.ResourceNotFoundException;
import euan.lessonbookingservice.exception.UsernameAlreadyExistsException;
import euan.lessonbookingservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToResponseDto(user);
    }

    public Long getUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    public UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if username is being changed and if it already exists
        if (!existingUser.getUsername().equals(userUpdateRequest.getUsername()) &&
                userRepository.existsByUsername(userUpdateRequest.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists: " + userUpdateRequest.getUsername());
        }

        existingUser.setUsername(userUpdateRequest.getUsername());
        existingUser.setRole(userUpdateRequest.getRole());

        User updatedUser = userRepository.save(existingUser);
        return convertToResponseDto(updatedUser);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponse convertToResponseDto(User user) {
        UserResponse userResponseDto = new UserResponse();
        userResponseDto.setId(user.getId());
        userResponseDto.setUsername(user.getUsername());
        userResponseDto.setRole(user.getRole());
        return userResponseDto;
    }
}