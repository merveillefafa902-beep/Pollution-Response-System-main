package org.rrc.pollution_response_system.repository;

import org.rrc.pollution_response_system.entity.PollutionCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PollutionCaseRepository extends JpaRepository<PollutionCase, Long> {

    List<PollutionCase> findByInvestigationStatus(PollutionCase.InvestigationStatus investigationStatus);

    List<PollutionCase> findByAssignedAuthority_Id(Long authorityId);

    List<PollutionCase> findByRegion_Id(Long regionId);

    List<PollutionCase> findByReporter(String reporter);

    // Analytics: counts by InvestigationStatus
    @Query("SELECT r.investigationStatus, COUNT(r) FROM PollutionCase r GROUP BY r.investigationStatus")
    List<Object[]> countGroupedByStatus();

    // Analytics: counts by pollution type
    @Query("SELECT r.pollutionCategory, COUNT(r) FROM PollutionCase r GROUP BY r.pollutionCategory")
    List<Object[]> countGroupedByType();

    // Analytics: counts by region name (null-safe using COALESCE)
    @Query("SELECT COALESCE(r.region.name, 'Unassigned'), COUNT(r) FROM PollutionCase r GROUP BY r.region.name")
    List<Object[]> countGroupedByRegion();

    // Analytics: counts by severity
    @Query("SELECT r.severity, COUNT(r) FROM PollutionCase r GROUP BY r.severity")
    List<Object[]> countGroupedBySeverity();

    // Fetch reports since a start time (for timeseries aggregation in service layer)
    List<PollutionCase> findByReportedAtGreaterThanEqual(LocalDateTime start);

    // Potential duplicates within time window (bounding-box search) - more efficient than scanning all
    @Query("SELECT r FROM PollutionCase r WHERE r.reportedAt BETWEEN :start AND :end AND ABS(r.latitude - :lat) < :delta AND ABS(r.longitude - :lon) < :delta")
    List<PollutionCase> findPotentialDuplicates(@Param("lat") Double lat,
                                                 @Param("lon") Double lon,
                                                 @Param("delta") Double delta,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
