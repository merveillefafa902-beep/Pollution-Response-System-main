package org.rrc.pollution_response_system.repository;

import org.rrc.pollution_response_system.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByName(String name);
}
