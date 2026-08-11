package euan.bookingservice.resources.service;

import euan.bookingservice.resources.model.OccupiedInterval;
import euan.bookingservice.resources.port.BookingAvailabilityLookup;
import euan.bookingservice.resources.dto.request.AvailabilityRuleRequest;
import euan.bookingservice.resources.dto.request.AvailabilityRuleUpdateRequest;
import euan.bookingservice.resources.dto.response.AvailabilityResponse;
import euan.bookingservice.resources.dto.response.AvailabilityRuleResponse;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.entity.ResourceAvailabilityRule;
import euan.bookingservice.resources.exception.*;
import euan.bookingservice.resources.repository.ResourceAvailabilityRuleRepository;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.resources.port.UserLookup;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class AvailabilityService {

    private final ResourceAvailabilityRuleRepository availabilityRuleRepository;
    private final UserLookup userLookup;
    private final ResourceRepository resourceRepository;
    private final BookingAvailabilityLookup bookingAvailabilityLookup;
    private final AvailabilityCalculator availabilityCalculator;

    public AvailabilityService(
            ResourceAvailabilityRuleRepository availabilityRuleRepository,
            UserLookup userLookup,
            ResourceRepository resourceRepository,
            BookingAvailabilityLookup bookingAvailabilityLookup,
            AvailabilityCalculator availabilityCalculator
    ) {
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.userLookup = userLookup;
        this.resourceRepository = resourceRepository;
        this.bookingAvailabilityLookup = bookingAvailabilityLookup;
        this.availabilityCalculator = availabilityCalculator;
    }

    public AvailabilityRuleResponse createAvailabilityRule(
            Long resourceId,
            AvailabilityRuleRequest request
    ) {
        Resource resource = getResource(resourceId);

        verifyWriteAccess(resource);

        validateRule(request.getStartTime(), request.getEndTime());

        if (!resourceId.equals(request.getResourceId())) {
            throw new InvalidAvailabilityRuleException(
                    "Resource ID in request does not match path resource ID"
            );
        }

        List<ResourceAvailabilityRule> existingRules =
                availabilityRuleRepository.findByResourceIdAndDayOfWeek(
                        resourceId,
                        request.getDayOfWeek()
                );

        validateNoOverlap(
                request.getStartTime(),
                request.getEndTime(),
                request.getEffectiveFrom(),
                request.getEffectiveTo(),
                existingRules,
                null
        );

        ResourceAvailabilityRule rule = new ResourceAvailabilityRule();

        rule.setResource(resource);
        rule.setDayOfWeek(request.getDayOfWeek());
        rule.setStartTime(request.getStartTime());
        rule.setEndTime(request.getEndTime());
        rule.setEffectiveFrom(request.getEffectiveFrom());
        rule.setEffectiveTo(request.getEffectiveTo());

        ResourceAvailabilityRule saved =
                availabilityRuleRepository.save(rule);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRuleResponse> getAvailabilityRules(
            Long resourceId
    ) {
        getResource(resourceId);

        return availabilityRuleRepository.findByResourceId(resourceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AvailabilityRuleResponse updateAvailabilityRule(
            Long resourceId,
            Long ruleId,
            AvailabilityRuleUpdateRequest request
    ) {
        Resource resource = getResource(resourceId);

        verifyWriteAccess(resource);

        ResourceAvailabilityRule rule =
                availabilityRuleRepository.findById(ruleId)
                        .orElseThrow(() ->
                                new AvailabilityRuleNotFoundException(
                                        "Availability rule not found: " + ruleId
                                )
                        );

        if (!rule.getResource().getId().equals(resourceId)) {
            throw new AvailabilityRuleNotFoundException(
                    "Availability rule not found: " + ruleId
            );
        }

        validateRule(request.getStartTime(), request.getEndTime());

        List<ResourceAvailabilityRule> existingRules =
                availabilityRuleRepository.findByResourceIdAndDayOfWeekAndIdNot(
                        resourceId,
                        request.getDayOfWeek(),
                        ruleId
                );

        validateNoOverlap(
                request.getStartTime(),
                request.getEndTime(),
                request.getEffectiveFrom(),
                request.getEffectiveTo(),
                existingRules,
                ruleId
        );

        rule.setDayOfWeek(request.getDayOfWeek());
        rule.setStartTime(request.getStartTime());
        rule.setEndTime(request.getEndTime());
        rule.setEffectiveFrom(request.getEffectiveFrom());
        rule.setEffectiveTo(request.getEffectiveTo());

        return toResponse(availabilityRuleRepository.save(rule));
    }

    public void deleteAvailabilityRule(
            Long resourceId,
            Long ruleId
    ) {
        Resource resource = getResource(resourceId);

        verifyWriteAccess(resource);

        ResourceAvailabilityRule rule =
                availabilityRuleRepository.findById(ruleId)
                        .orElseThrow(() ->
                                new AvailabilityRuleNotFoundException(
                                        "Availability rule not found: " + ruleId
                                )
                        );

        if (!rule.getResource().getId().equals(resourceId)) {
            throw new AvailabilityRuleNotFoundException(
                    "Availability rule not found: " + ruleId
            );
        }

        availabilityRuleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailableSlots(
            Long resourceId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Resource resource = getResource(resourceId);

        if (from == null || to == null) {
            throw new InvalidAvailabilityRuleException(
                    "From and to are required"
            );
        }

        if (!from.isBefore(to)) {
            throw new InvalidAvailabilityRuleException(
                    "From must be before to"
            );
        }

        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();

        List<ResourceAvailabilityRule> rules =
                availabilityRuleRepository.findByResourceId(resourceId);

        List<OccupiedInterval> occupiedIntervals =
                bookingAvailabilityLookup.findOccupiedIntervals(
                        resourceId,
                        from,
                        to
                );

        return availabilityCalculator.calculate(
                fromDate,
                toDate,
                rules,
                occupiedIntervals,
                resource.getCapacity()
        );
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(
            Long resourceId,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) {
        Resource resource = getResource(resourceId);

        if (startTime == null || endTime == null) {
            throw new InvalidAvailabilityRuleException(
                    "Start time and end time are required"
            );
        }

        if (!startTime.isBefore(endTime)) {
            throw new InvalidAvailabilityRuleException(
                    "Start time must be before end time"
            );
        }

        List<ResourceAvailabilityRule> rules =
                availabilityRuleRepository.findByResourceId(resourceId);

        List<OccupiedInterval> occupiedIntervals =
                bookingAvailabilityLookup.findOccupiedIntervals(
                        resourceId,
                        startTime,
                        endTime
                );

        AvailabilityResponse availability =
                availabilityCalculator.calculate(
                        startTime.toLocalDate(),
                        endTime.toLocalDate(),
                        rules,
                        occupiedIntervals,
                        resource.getCapacity()
                );

        return availability.getSlots()
                .stream()
                .anyMatch(slot ->
                        !startTime.isBefore(slot.getStartTime())
                                && !endTime.isAfter(slot.getEndTime())
                );
    }

    @Transactional(readOnly = true)
    public boolean isAvailableExcludingBooking(
            Long resourceId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Long bookingId
    ) {
        Resource resource = getResource(resourceId);

        if (startTime == null || endTime == null) {
            throw new InvalidAvailabilityRuleException(
                    "Start time and end time are required"
            );
        }

        if (!startTime.isBefore(endTime)) {
            throw new InvalidAvailabilityRuleException(
                    "Start time must be before end time"
            );
        }

        List<ResourceAvailabilityRule> rules =
                availabilityRuleRepository.findByResourceId(resourceId);

        List<OccupiedInterval> occupiedIntervals =
                bookingAvailabilityLookup.findOccupiedIntervalsExcludingBooking(
                        resourceId,
                        startTime,
                        endTime,
                        bookingId
                );

        AvailabilityResponse availability =
                availabilityCalculator.calculate(
                        startTime.toLocalDate(),
                        endTime.toLocalDate(),
                        rules,
                        occupiedIntervals,
                        resource.getCapacity()
                );

        return availability.getSlots()
                .stream()
                .anyMatch(slot ->
                        !startTime.isBefore(slot.getStartTime())
                                && !endTime.isAfter(slot.getEndTime())
                );
    }

    private Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found: " + resourceId
                        )
                );
    }

    private void validateRule(
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {
        if (startTime == null || endTime == null) {
            throw new InvalidAvailabilityRuleException(
                    "Start time and end time are required"
            );
        }

        if (!startTime.isBefore(endTime)) {
            throw new InvalidAvailabilityRuleException(
                    "Start time must be before end time"
            );
        }
    }

    private void validateNoOverlap(
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            List<ResourceAvailabilityRule> existingRules,
            Long excludedRuleId
    ) {
        validateEffectiveDates(effectiveFrom, effectiveTo);

        for (ResourceAvailabilityRule existing : existingRules) {

            if (excludedRuleId != null &&
                    excludedRuleId.equals(existing.getId())) {
                continue;
            }

            if (!effectiveDatesOverlap(
                    effectiveFrom,
                    effectiveTo,
                    existing.getEffectiveFrom(),
                    existing.getEffectiveTo()
            )) {
                continue;
            }

            /*
             * Adjacent windows are allowed:
             *
             * 09:00 - 10:00
             * 10:00 - 11:00
             *
             * Actual overlap requires:
             *
             * start < existing.end
             * AND
             * end > existing.start
             */
            boolean timeOverlap =
                    startTime.isBefore(existing.getEndTime()) &&
                            endTime.isAfter(existing.getStartTime());

            if (timeOverlap) {
                throw new InvalidAvailabilityRuleException(
                        "Availability rule overlaps an existing rule"
                );
            }
        }
    }

    private void validateEffectiveDates(
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        if (effectiveFrom != null &&
                effectiveTo != null &&
                effectiveFrom.isAfter(effectiveTo)) {

            throw new InvalidAvailabilityRuleException(
                    "Effective from date must be before or equal to effective to date"
            );
        }
    }

    private boolean effectiveDatesOverlap(
            LocalDate firstFrom,
            LocalDate firstTo,
            LocalDate secondFrom,
            LocalDate secondTo
    ) {
        LocalDate firstStart =
                firstFrom != null ? firstFrom : LocalDate.MIN;

        LocalDate firstEnd =
                firstTo != null ? firstTo : LocalDate.MAX;

        LocalDate secondStart =
                secondFrom != null ? secondFrom : LocalDate.MIN;

        LocalDate secondEnd =
                secondTo != null ? secondTo : LocalDate.MAX;

        return !firstStart.isAfter(secondEnd) &&
                !secondStart.isAfter(firstEnd);
    }

    private void verifyWriteAccess(Resource resource) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (isAdmin) {
            return;
        }

        boolean isOrganizer = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ORGANIZER"::equals);

        if (!isOrganizer) {
            throw new AccessDeniedException(
                    "Only admins or resource owners can modify availability"
            );
        }

        Long authenticatedUserId =
                userLookup.findUserIdByUsername(authentication.getName())
                        .orElseThrow(() ->
                                new AccessDeniedException("Authenticated user not found")
                        );

        if (!authenticatedUserId.equals(resource.getOwnerId())) {
            throw new AccessDeniedException("You do not own this resource");
        }
    }

    private AvailabilityRuleResponse toResponse(
            ResourceAvailabilityRule rule
    ) {
        return AvailabilityRuleResponse.builder()
                .id(rule.getId())
                .resourceId(rule.getResource().getId())
                .dayOfWeek(rule.getDayOfWeek())
                .startTime(rule.getStartTime())
                .endTime(rule.getEndTime())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .createdBy(rule.getCreatedBy())
                .updatedBy(rule.getUpdatedBy())
                .build();
    }
}