package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.service.UserService;
import org.rrc.pollution_response_system.service.FileStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final org.rrc.pollution_response_system.validation.UserValidator userValidator;

    public UserController(UserService userService, FileStorageService fileStorageService,
            org.rrc.pollution_response_system.validation.UserValidator userValidator) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.userValidator = userValidator;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            userValidator.validateUser(user.getEmail(), user.getPhone());
            User savedUser = userService.saveUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred while creating the user: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ✅ Get all users (any authenticated role)
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // ✅ Get user by ID (any authenticated role)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Optional<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // ✅ Update InvestigationStatus (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/InvestigationStatus")
    public User updateUserStatus(@PathVariable Long id, @RequestParam String InvestigationStatus) {
        return userService.updateUserStatus(id, InvestigationStatus);
    }

    // ✅ Update User (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User updatedData) {
        if (updatedData.getEmail() != null && updatedData.getPhone() != null) {
             userValidator.validateUser(updatedData.getEmail(), updatedData.getPhone());
        }
        return userService.updateUser(id, updatedData);
    }

    // ✅ Delete user (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    // ✅ Admin: reset password and email new temporary password
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }

    // ✅ Self-service: get current profile
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/current")
    public Optional<User> current(Authentication auth) {
        return userService.getUserByUsername(auth.getName());
    }

    // ✅ Self-service: update profile (fullName, email, phone)
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/current/profile")
    public User updateProfile(Authentication auth,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        if (email != null && !email.isEmpty() && phone != null && !phone.isEmpty()) {
            userValidator.validateUser(email, phone);
        } else if (email != null && !email.isEmpty()) {
            if (!userValidator.isValidEmail(email))
                throw new IllegalArgumentException("Invalid email format");
        } else if (phone != null && !phone.isEmpty()) {
            if (!userValidator.isValidPhone(phone))
                throw new IllegalArgumentException("Invalid phone number");
        }
        return userService.updateProfile(auth.getName(), fullName, email, phone);
    }

    // ✅ Self-service: change password
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/current/password")
    public boolean changePassword(Authentication auth,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        return userService.changePassword(auth.getName(), currentPassword, newPassword);
    }

    // ✅ Self-service: change username
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/current/username")
    public String changeUsername(Authentication auth,
            @RequestParam String newUsername) {
        boolean ok = userService.changeUsername(auth.getName(), newUsername);
        if (ok)
            return "UPDATED";
        throw new IllegalArgumentException("Username not available or invalid");
    }

    // ✅ Upload profile image
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/current/profile-image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file, Authentication auth) {
        try {
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please select a file to upload");
                return ResponseEntity.badRequest().body(error);
            }

            // Get current user
            Optional<User> userOpt = userService.getUserByUsername(auth.getName());
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.badRequest().body(error);
            }

            User user = userOpt.get();

            // Delete old profile image if exists
            String oldImagePath = user.getProfileImagePath();
            if (oldImagePath != null && !oldImagePath.isEmpty()) {
                fileStorageService.deleteProfileImage(oldImagePath);
            }

            // Store new image
            String imagePath = fileStorageService.storeProfileImage(file, user.getId());

            // Update user record
            userService.updateProfileImage(auth.getName(), imagePath);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile image uploaded successfully");
            response.put("imagePath", imagePath);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ✅ Upload profile image for specific user (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/profile-image")
    public ResponseEntity<?> uploadUserProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please select a file to upload");
                return ResponseEntity.badRequest().body(error);
            }

            Optional<User> userOpt = userService.getUserById(id);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.badRequest().body(error);
            }

            User user = userOpt.get();
            String oldImagePath = user.getProfileImagePath();
            if (oldImagePath != null && !oldImagePath.isEmpty()) {
                fileStorageService.deleteProfileImage(oldImagePath);
            }

            String imagePath = fileStorageService.storeProfileImage(file, user.getId());
            userService.updateProfileImage(user.getUsername(), imagePath);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile image updated successfully");
            response.put("imagePath", imagePath);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ✅ Delete profile image
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/current/profile-image")
    public ResponseEntity<?> deleteProfileImage(Authentication auth) {
        try {
            String currentImagePath = userService.getProfileImagePath(auth.getName());

            if (currentImagePath == null || currentImagePath.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No profile image to delete");
                return ResponseEntity.badRequest().body(error);
            }

            // Delete file
            fileStorageService.deleteProfileImage(currentImagePath);

            // Update user record
            userService.updateProfileImage(auth.getName(), null);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile image deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete image: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
