package org.rrc.pollution_response_system.service;

import org.rrc.pollution_response_system.entity.Region;
import org.rrc.pollution_response_system.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    // ✅ Create or update a region
    public Region saveRegion(Region region) {
        return regionRepository.save(region);
    }

    // ✅ Get all regions
    public List<Region> getAllRegions() {
        return regionRepository.findAll();
    }

    // ✅ Get region by ID
    public Optional<Region> getRegionById(Long id) {
        return regionRepository.findById(id);
    }

    // ✅ Delete region (admin only)
    public void deleteRegion(Long id) {
        regionRepository.deleteById(id);
    }
}
