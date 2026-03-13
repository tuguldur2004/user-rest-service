package com.example.userrest.dto;

/**
 * Holds the result parsed from the SOAP ValidateToken response.
 *
 * SOAP response structure (from UserAuth.wsdl):
 * <ValidateTokenResponse>
 * <valid>true</valid>
 * <userId>42</userId>
 * <username>johndoe</username>
 * </ValidateTokenResponse>
 */
public class TokenValidationResult {

    private boolean valid;
    private int userId;
    private String username;
    private String role;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
