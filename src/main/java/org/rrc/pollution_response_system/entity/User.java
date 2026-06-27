package org.rrc.pollution_response_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @jakarta.validation.constraints.Pattern(regexp = "^[\\w-\\.]+@[a-zA-Z][a-zA-Z-]*(\\.[a-zA-Z][a-zA-Z-]*)*\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    private String email;

    @jakarta.validation.constraints.Pattern(regexp = "^07[2389]\\d{7}$", message = "Invalid phone number")
    private String phone;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    // Multi-role support (ADMIN can also have other delegated roles, etc.)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Set<Role> roles = new HashSet<>();


    @Column(nullable = false)
    private String status = "ACTIVE";

    // Relationships
    @JsonIgnore
    @OneToMany(mappedBy = "assignedAuthority")
    private List<PollutionCase> assignedReports;


    // Enums
    public enum Role {
        ADMIN, ENVIRONMENTAL_AUTHORITY, CITIZEN
    }


    // Constructors
    public User() {
    }

    public User(String username, String password, String fullName, String email,
            String phone, Set<Role> roles) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.roles = roles != null ? roles : new HashSet<>();
    }

    // Utility methods
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }
}
