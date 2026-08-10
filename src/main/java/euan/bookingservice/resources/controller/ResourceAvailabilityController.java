package euan.bookingservice.resources.controller;

import euan.bookingservice.resources.dto.request.AvailabilityRuleRequest;
import euan.bookingservice.resources.dto.request.AvailabilityRuleUpdateRequest;
import euan.bookingservice.resources.dto.response.AvailabilityResponse;
import euan.bookingservice.resources.dto.response.AvailabilityRuleResponse;
import euan.bookingservice.resources.service.AvailabilityService;
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

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/resources/{id}/availability")
@Tag(name = "Availability", description = "Resource availability management API")
@SecurityRequirement(name = "bearerAuth")
public class ResourceAvailabilityController {

    private final AvailabilityService availabilityService;

    public ResourceAvailabilityController(
            AvailabilityService availabilityService
    ) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    @Operation(
            summary = "Create an availability rule",
            description = "Create a new availability rule for a resource."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Availability rule created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AvailabilityRuleResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            )
    })
    public ResponseEntity<AvailabilityRuleResponse> createAvailabilityRule(
            @Parameter(
                    description = "Resource ID",
                    required = true
            )
            @PathVariable Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Availability rule details",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = AvailabilityRuleRequest.class
                            )
                    )
            )
            @Valid @RequestBody AvailabilityRuleRequest request
    ) {
        AvailabilityRuleResponse response =
                availabilityService.createAvailabilityRule(id, request);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/rules")
    @Operation(
            summary = "Get availability rules",
            description = "Retrieve availability rules for a resource."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved availability rules",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AvailabilityRuleResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<List<AvailabilityRuleResponse>> getAvailabilityRules(
            @Parameter(
                    description = "Resource ID",
                    required = true
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                availabilityService.getAvailabilityRules(id)
        );
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    @Operation(
            summary = "Update an availability rule",
            description = "Update an existing availability rule."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Availability rule updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AvailabilityRuleResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Availability rule not found",
                    content = @Content
            )
    })
    public ResponseEntity<AvailabilityRuleResponse> updateAvailabilityRule(
            @Parameter(
                    description = "Resource ID",
                    required = true
            )
            @PathVariable Long id,

            @Parameter(
                    description = "Availability rule ID",
                    required = true
            )
            @PathVariable Long ruleId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated availability rule details",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = AvailabilityRuleUpdateRequest.class
                            )
                    )
            )
            @Valid @RequestBody AvailabilityRuleUpdateRequest request
    ) {
        return ResponseEntity.ok(
                availabilityService.updateAvailabilityRule(
                        id,
                        ruleId,
                        request
                )
        );
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    @Operation(
            summary = "Delete an availability rule",
            description = "Delete an availability rule by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Availability rule deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Availability rule not found",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deleteAvailabilityRule(
            @Parameter(
                    description = "Resource ID",
                    required = true
            )
            @PathVariable Long id,

            @Parameter(
                    description = "Availability rule ID",
                    required = true
            )
            @PathVariable Long ruleId
    ) {
        availabilityService.deleteAvailabilityRule(id, ruleId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Get resource availability",
            description = "Retrieve calculated availability for a resource within a date range."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved availability",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AvailabilityResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @Parameter(
                    description = "Resource ID",
                    required = true
            )
            @PathVariable Long id,

            @Parameter(
                    description = "Start date/time",
                    required = true
            )
            @RequestParam OffsetDateTime from,

            @Parameter(
                    description = "End date/time",
                    required = true
            )
            @RequestParam OffsetDateTime to
    ) {
        return ResponseEntity.ok(
                availabilityService.getAvailableSlots(
                        id,
                        from,
                        to
                )
        );
    }
}
