package org.rrc.pollution_response_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/test")
    public String test() {
        System.out.println("✅ /test endpoint called!");
        return "dashboard";
    }
}
