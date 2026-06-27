package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // templates/login.html
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register"; // templates/register.html
    }

    @Autowired
    private org.rrc.pollution_response_system.validation.UserValidator userValidator;

    @PostMapping("/register")
    public String registerUser(@RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String role,
            Model model) {

        try {
            userValidator.validateUser(email, phone);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            User user = new User();
            user.setFullName(fullName);
            user.setUsername(username);
            user.setEmail(email);
            user.setPhone(phone);
            model.addAttribute("user", user);
            return "register";
        }

        if (userService.getUserByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists!");
            model.addAttribute("user", new User());
            return "register";
        }

        // Create new user with CITIZEN role only
        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus("ACTIVE");

        // Force CITIZEN role for public registration
        user.addRole(User.Role.CITIZEN);

        userService.saveUser(user);

        return "redirect:/login?registered=true";
    }
}
