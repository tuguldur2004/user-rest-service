package com.example.userrest.dto;

import jakarta.validation.constraints.Email;

/**
 * Request body for PUT /users/:id — partial update (all fields optional).
 * Only non-null fields are applied to the stored profile.
 */
public class UpdateUserRequest {

    @Email(message = "Must be a valid email address")
    private String email;

    private String name;
    private String bio;
    private String phone;
    private String avatar;
    private String location;
    private String website;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
