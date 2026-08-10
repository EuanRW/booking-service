package euan.bookingservice.resources.repository;

import euan.bookingservice.resources.entity.ResourceAvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface ResourceAvailabilityRuleRepository extends JpaRepository<ResourceAvailabilityRule, Long> {

    List<ResourceAvailabilityRule> findByResourceId(Long resourceId);

    List<ResourceAvailabilityRule> findByResourceIdAndDayOfWeek(Long resourceId, DayOfWeek dayOfWeek);

    List<ResourceAvailabilityRule> findByResourceIdAndDayOfWeekAndIdNot(
            Long resourceId,
            DayOfWeek dayOfWeek,
            Long id
    );
}