package euan.lessonbookingservice.controller;

import euan.lessonbookingservice.dto.request.ResourceRequest;
import euan.lessonbookingservice.dto.response.ResourceResponse;
import euan.lessonbookingservice.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resources")
@Tag(name = "Resources", description = "Generic resource management API")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    @Operation(summary = "Create a new resource", description = "Create a generic resource such as a lesson, room, or equipment booking target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Resource created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    public ResponseEntity<ResourceResponse> createResource(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Resource details", required = true, content = @Content(schema = @Schema(implementation = ResourceRequest.class)))
            @Valid @RequestBody ResourceRequest resourceRequest) {
        ResourceResponse createdResource = resourceService.createResource(resourceRequest);
        return ResponseEntity.status(201).body(createdResource);
    }

    @GetMapping
    @Operation(summary = "Get all resources", description = "Retrieve a list of all generic resources.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved resources", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceResponse.class)))
    })
    public ResponseEntity<List<ResourceResponse>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a resource by ID", description = "Retrieve a specific resource by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resource found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    public ResponseEntity<ResourceResponse> getResourceById(
            @Parameter(description = "Resource ID", required = true)
            @PathVariable Long id) {
        return resourceService.getResourceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    @Operation(summary = "Update a resource", description = "Update an existing resource.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resource updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    public ResponseEntity<ResourceResponse> updateResource(
            @Parameter(description = "Resource ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated resource details", required = true, content = @Content(schema = @Schema(implementation = ResourceRequest.class)))
            @Valid @RequestBody ResourceRequest resourceRequest) {
        return resourceService.updateResource(id, resourceRequest)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    @Operation(summary = "Delete a resource", description = "Delete a resource by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Resource deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    public ResponseEntity<Void> deleteResource(
            @Parameter(description = "Resource ID", required = true)
            @PathVariable Long id) {
        if (resourceService.deleteResource(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
