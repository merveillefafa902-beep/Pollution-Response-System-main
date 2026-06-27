package org.rrc.pollution_response_system.config;

import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.entity.Region;
import org.rrc.pollution_response_system.repository.PollutionCaseRepository;
import org.rrc.pollution_response_system.repository.RegionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PollutionCaseRepository reportRepository;
    private final RegionRepository regionRepository;

    public DataSeeder(PollutionCaseRepository reportRepository, RegionRepository regionRepository) {
        this.reportRepository = reportRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRegions();
        seedSampleReports();
    }

    private void seedRegions() {
        if (regionRepository.count() > 0) {
            System.out.println("✅ Regions already exist, skipping seed");
            return;
        }

        System.out.println("🌍 Seeding regions with Kigali coordinates...");

        // Kigali Districts with realistic coordinates
        List<Region> regions = List.of(
                createRegion("Gasabo", "Northern Kigali district", -1.9358, 30.1057)
        );

        regionRepository.saveAll(regions);
        System.out.println("✅ Seeded " + regions.size() + " regions");
    }

    private Region createRegion(String name, String description, double lat, double lon) {
        Region region = new Region();
        region.setName(name);
        region.setDescription(description);
        region.setLatitude(lat);
        region.setLongitude(lon);
        return region;
    }

    private void seedSampleReports() {
        // Feature removed to start database from zero
        System.out.println("✅ Sample reports seeding is disabled.");
    }
}
