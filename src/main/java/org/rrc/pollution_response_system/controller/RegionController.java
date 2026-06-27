package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.entity.Region;
import org.rrc.pollution_response_system.service.RegionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/regions")
@CrossOrigin(origins = "*")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    // ✅ Admin/Authority: Create region
    @PreAuthorize("hasAnyRole('ADMIN','ENVIRONMENTAL_AUTHORITY')")
    @PostMapping
    public Region createRegion(@RequestBody Region region) {
        return regionService.saveRegion(region);
    }

    // ✅ Admin/Authority: Update region
    @PreAuthorize("hasAnyRole('ADMIN','ENVIRONMENTAL_AUTHORITY')")
    @PutMapping("/{id}")
    public Region updateRegion(@PathVariable Long id, @RequestBody Region region) {
        region.setId(id);
        return regionService.saveRegion(region);
    }

    // ✅ Get all regions
    @GetMapping
    public List<Region> getAllRegions() {
        return regionService.getAllRegions();
    }

    // ✅ Get region by ID
    @GetMapping("/{id}")
    public Optional<Region> getRegionById(@PathVariable Long id) {
        return regionService.getRegionById(id);
    }

    // ✅ Admin/Authority: Delete region
    @PreAuthorize("hasAnyRole('ADMIN','ENVIRONMENTAL_AUTHORITY')")
    @DeleteMapping("/{id}")
    public void deleteRegion(@PathVariable Long id) {
        regionService.deleteRegion(id);
    }
}
