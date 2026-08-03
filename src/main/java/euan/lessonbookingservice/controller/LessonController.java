package euan.lessonbookingservice.controller;

import euan.lessonbookingservice.dto.request.LessonRequest;
import euan.lessonbookingservice.dto.request.ResourceRequest;
import euan.lessonbookingservice.dto.response.LessonResponse;
import euan.lessonbookingservice.entity.ResourceType;
import euan.lessonbookingservice.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lessons")
@Tag(name = "Lessons", description = "Legacy lesson endpoints backed by generic resources")
@SecurityRequirement(name = "bearerAuth")
public class LessonController {
    private final ResourceService resourceService;

    public LessonController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
        @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
        @Operation(summary = "Create a new lesson", description = "Create a lesson-style resource. Only accessible by ADMIN or ORGANIZER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lesson created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    public ResponseEntity<LessonResponse> createLesson(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Lesson details", required = true, content = @Content(schema = @Schema(implementation = LessonRequest.class)))
            @RequestBody LessonRequest lessonRequest) {
        ResourceRequest resourceRequest = mapToResourceRequest(lessonRequest);
        return ResponseEntity.status(201).body(mapToLessonResponse(resourceService.createLessonResourceFromLegacyRequest(resourceRequest)));
    }

    @GetMapping
    @Operation(summary = "Get all lessons", description = "Retrieve a list of all lesson-style resources.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lessons", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class)))
    public ResponseEntity<List<LessonResponse>> getAllLessons() {
        List<LessonResponse> responses = resourceService.getAllResources().stream()
                .filter(resource -> resource.getResourceType() == ResourceType.LESSON)
                .map(this::mapToLessonResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a lesson by ID", description = "Retrieve a specific lesson-style resource by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lesson not found", content = @Content)
    })
    public ResponseEntity<LessonResponse> getLessonById(
            @Parameter(description = "Lesson ID", required = true)
            @PathVariable Long id) {
        return resourceService.getResourceById(id)
                .filter(resource -> resource.getResourceType() == ResourceType.LESSON)
                .map(this::mapToLessonResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
        @Operation(summary = "Update a lesson", description = "Update an existing lesson-style resource. Only accessible by ADMIN or ORGANIZER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lesson not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    public ResponseEntity<LessonResponse> updateLesson(
            @Parameter(description = "Lesson ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated lesson details", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonRequest.class)))
            @RequestBody LessonRequest lessonRequest) {
        ResourceRequest resourceRequest = mapToResourceRequest(lessonRequest);
        return resourceService.updateResource(id, resourceRequest)
                .filter(resource -> resource.getResourceType() == ResourceType.LESSON)
                .map(this::mapToLessonResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
        @Operation(summary = "Delete a lesson", description = "Delete a lesson-style resource by its ID. Only accessible by ADMIN or ORGANIZER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lesson deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    public ResponseEntity<Void> deleteLesson(
            @Parameter(description = "Lesson ID", required = true)
            @PathVariable Long id) {
        if (resourceService.deleteResource(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private ResourceRequest mapToResourceRequest(LessonRequest lessonRequest) {
        ResourceRequest resourceRequest = new ResourceRequest();
        resourceRequest.setTitle(lessonRequest.getTitle());
        resourceRequest.setDescription(lessonRequest.getDescription());
        resourceRequest.setOrganizerId(lessonRequest.getTeacherId());
        resourceRequest.setScheduledTime(lessonRequest.getScheduledTime());
        resourceRequest.setResourceType(ResourceType.LESSON);
        return resourceRequest;
    }

    private LessonResponse mapToLessonResponse(euan.lessonbookingservice.dto.response.ResourceResponse resource) {
        LessonResponse lessonResponse = new LessonResponse();
        lessonResponse.setId(resource.getId());
        lessonResponse.setTitle(resource.getTitle());
        lessonResponse.setDescription(resource.getDescription());
        lessonResponse.setTeacherId(resource.getOrganizerId());
        lessonResponse.setScheduledTime(resource.getScheduledTime());
        return lessonResponse;
    }
}
