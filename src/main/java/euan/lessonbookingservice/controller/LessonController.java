package euan.lessonbookingservice.controller;

import euan.lessonbookingservice.dto.request.LessonRequest;
import euan.lessonbookingservice.dto.response.LessonResponse;
import euan.lessonbookingservice.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lessons")
@Tag(name = "Lessons", description = "Lesson management API")
@SecurityRequirement(name = "bearerAuth")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Create a new lesson", description = "Create a new lesson. Only accessible by ADMIN or TEACHER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lesson created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content)
    })
    public ResponseEntity<LessonResponse> createLesson(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Lesson details", required = true,
                    content = @Content(schema = @Schema(implementation = LessonRequest.class)))
            @RequestBody LessonRequest lessonRequest) {
        LessonResponse createdLesson = lessonService.createLesson(lessonRequest);
        return ResponseEntity.status(201).body(createdLesson);
    }

    @GetMapping
    @Operation(summary = "Get all lessons", description = "Retrieve a list of all lessons.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lessons",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class)))
    public ResponseEntity<List<LessonResponse>> getAllLessons() {
        return ResponseEntity.ok(lessonService.getAllLessons());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a lesson by ID", description = "Retrieve a specific lesson by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lesson not found",
                    content = @Content)
    })
    public ResponseEntity<LessonResponse> getLessonById(
            @Parameter(description = "Lesson ID", required = true)
            @PathVariable Long id) {
        return lessonService.getLessonById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Update a lesson", description = "Update an existing lesson. Only accessible by ADMIN or TEACHER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lesson not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content)
    })
    public ResponseEntity<LessonResponse> updateLesson(
            @Parameter(description = "Lesson ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated lesson details", required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonRequest.class)))
            @RequestBody LessonRequest lessonRequest) {
        return lessonService.updateLesson(id, lessonRequest)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Delete a lesson", description = "Delete a lesson by its ID. Only accessible by ADMIN or TEACHER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lesson deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteLesson(
            @Parameter(description = "Lesson ID", required = true)
            @PathVariable Long id) {
        if (lessonService.deleteLesson(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}