package org.rrc.pollution_response_system.service;

import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@example.com}")
    private String mailFrom;

    @Value("${app.security.password.reset.length:12}")
    private int passwordLength;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    // ✅ Create or update user
    public User saveUser(User user) {
        // If password not provided, generate a random one and send email
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            String raw = generateRandomPassword(passwordLength);
            user.setPassword(passwordEncoder.encode(raw));
            System.out.println("\n=======================================================");
            System.out.println("NEW USER CREATED: " + user.getUsername());
            System.out.println("TEMPORARY PASSWORD: " + raw);
            System.out.println("=======================================================\n");
            sendOnboardingEmail(user.getEmail(), user.getUsername(), raw);
        } else {
            // ensure encoding if plain text leaked in
            if (!user.getPassword().startsWith("$2a") && !user.getPassword().startsWith("$2b")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of(User.Role.CITIZEN));
        }
        return userRepository.save(user);
    }

    // ✅ Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ✅ Get user by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // ✅ Get user by username
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // ✅ Get all users by role
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }



    // ✅ Activate or suspend user
    public User updateUserStatus(Long id, String newStatus) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(newStatus);
            return userRepository.save(user);
        }
        return null;
    }

    // ✅ Update user (Admin only)
    public User updateUser(Long id, User updatedData) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (updatedData.getFullName() != null) user.setFullName(updatedData.getFullName());
            if (updatedData.getUsername() != null) user.setUsername(updatedData.getUsername());
            if (updatedData.getEmail() != null) user.setEmail(updatedData.getEmail());
            if (updatedData.getPhone() != null) user.setPhone(updatedData.getPhone());
            if (updatedData.getRoles() != null && !updatedData.getRoles().isEmpty()) user.setRoles(updatedData.getRoles());
            if (updatedData.getStatus() != null) user.setStatus(updatedData.getStatus());
            return userRepository.save(user);
        }
        return null;
    }

    // Reset password (admin action)
    public String resetPassword(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        String raw = generateRandomPassword(passwordLength);
        user.setPassword(passwordEncoder.encode(raw));
        userRepository.save(user);
        System.out.println("\n=======================================================");
        System.out.println("PASSWORD RESET FOR: " + user.getUsername());
        System.out.println("NEW TEMPORARY PASSWORD: " + raw);
        System.out.println("=======================================================\n");
        sendPasswordResetEmail(user.getEmail(), user.getUsername(), raw);
        return raw; // return new raw password for confirmation (avoid logging)
    }

    // Self service password change
    public boolean changePassword(String username, String currentRaw, String newRaw) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        if (!passwordEncoder.matches(currentRaw, user.getPassword())) return false;
        user.setPassword(passwordEncoder.encode(newRaw));
        userRepository.save(user);
        return true;
    }

    // Self service profile update
    public User updateProfile(String username, String fullName, String email, String phone) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;
        if (fullName != null) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        return userRepository.save(user);
    }

    // Self service username change (requires uniqueness)
    public boolean changeUsername(String currentUsername, String newUsername) {
        if (newUsername == null || newUsername.isBlank()) return false;
        if (userRepository.findByUsername(newUsername).isPresent()) return false; // already taken
        User user = userRepository.findByUsername(currentUsername).orElse(null);
        if (user == null) return false;
        user.setUsername(newUsername);
        userRepository.save(user);
        return true;
    }

    private String generateRandomPassword(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$#!";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private void sendOnboardingEmail(String to, String username, String rawPassword) {
        if (to == null || to.isBlank()) return;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailFrom);
        msg.setTo(to);
        msg.setSubject("Your ENVIRONMENT-BASED POLLUTION REPORTING SYSTEM Account");
        msg.setText("Hello " + username + "\n\nYour account has been created.\nUsername: " + username + "\nTemporary Password: " + rawPassword + "\nPlease log in and change it immediately.\n\nRegards, ENVIRONMENT-BASED POLLUTION REPORTING SYSTEM Team");
        try {
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send onboarding email: " + e.getMessage());
        }
    }

    private void sendPasswordResetEmail(String to, String username, String rawPassword) {
        if (to == null || to.isBlank()) return;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailFrom);
        msg.setTo(to);
        msg.setSubject("Password Reset - ENVIRONMENT-BASED POLLUTION REPORTING SYSTEM");
        msg.setText("Hello " + username + "\n\nYour password has been reset.\nNew Temporary Password: " + rawPassword + "\nPlease log in and change it immediately.\n\nRegards, ENVIRONMENT-BASED POLLUTION REPORTING SYSTEM Team");
        try {
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    // ✅ Delete user
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    // ✅ Update profile image path
    public User updateProfileImage(String username, String imagePath) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        
        User user = userOpt.get();
        user.setProfileImagePath(imagePath);
        return userRepository.save(user);
    }
    
    // ✅ Get user's current profile image path
    public String getProfileImagePath(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::getProfileImagePath).orElse(null);
    }
}
