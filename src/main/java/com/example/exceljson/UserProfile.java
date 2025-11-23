package com.example.exceljson;

/**
 * Enum representing the two user profiles supported by the application:
 * IE (Implementation Engineer) - Full functionality mode
 * CI (Clinical Informatics) - Restricted mode with guided workflows
 */
public enum UserProfile {
    IE("Implementation Engineer"),
    CI("Clinical Informatics");
    
    private final String displayName;
    
    UserProfile(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
