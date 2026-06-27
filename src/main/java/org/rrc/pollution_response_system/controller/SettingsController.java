package org.rrc.pollution_response_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SettingsController {

    // Example system settings (you can later load from DB)
    private String backupSchedule = "Weekly (Sunday 2:00 AM)";
    private String[] pollutionCategorys = {"Fire", "Flood", "Accident", "Landslide"};

    // Display settings page
    @GetMapping("/settings")
    public String settingsPage(Model model) {
        model.addAttribute("backupSchedule", backupSchedule);
        model.addAttribute("pollutionCategorys", pollutionCategorys);
        return "settings";
    }

    // Handle form submission from settings.html
    @PostMapping("/settings/update")
    public String updateSettings(
            @RequestParam("backupSchedule") String schedule,
            @RequestParam("pollutionCategorys") String[] types,
            Model model) {

        this.backupSchedule = schedule;
        this.pollutionCategorys = types;

        model.addAttribute("message", "✅ Settings updated successfully!");
        model.addAttribute("backupSchedule", backupSchedule);
        model.addAttribute("pollutionCategorys", pollutionCategorys);

        return "settings";
    }
}
