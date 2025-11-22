package com.example.exceljson.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssignmentRoleValidator {

    // Pattern to match "VAssign: [Room]" or "VAssign:"
    // Uses [^,;\n]+ to capture everything until delimiter (consistent with VoiceGroupValidator)
    // This allows multi-word role names like "Room 101" or "ICU Pod A"
    private static final Pattern VASSIGN_PATTERN = Pattern.compile("(?i)(VAssign:\\s*)([^,;\\n]+)?");

    public static List<List<Segment>> parseAndValidateMultiLine(String text, Set<String> loadedAssignmentRoles) {
        List<List<Segment>> lineSegments = new ArrayList<>();
        if (text == null) {
            return lineSegments;
        }

        String[] lines = text.split("\\n");
        for (String line : lines) {
            lineSegments.add(parseAndValidate(line, loadedAssignmentRoles));
        }
        return lineSegments;
    }
    
    public static List<Segment> parseAndValidate(String text, Set<String> loadedAssignmentRoles) {
        List<Segment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        Matcher m = VASSIGN_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (m.find()) {
            // Text before the match
            if (m.start() > lastEnd) {
                segments.add(new Segment(text.substring(lastEnd, m.start()), ValidationStatus.PLAIN));
            }
            
            // The prefix (e.g. "VAssign: ")
            String prefix = m.group(1);
            segments.add(new Segment(prefix, ValidationStatus.PLAIN));
            
            // The role name - this is what we validate (may be null for "VAssign:" without a role)
            String roleName = m.group(2);
            if (roleName != null) {
                roleName = roleName.trim();
                
                // Clean role name for validation (remove trailing special chars)
                String nameToValidate = roleName.replaceAll("[^a-zA-Z0-9_\\-\\s]+$", "").trim();
                
                // Check if this specific role name exists in loaded roles (case-insensitive)
                boolean isValid = false;
                if (!nameToValidate.isEmpty()) {
                    for (String validRole : loadedAssignmentRoles) {
                        if (validRole.equalsIgnoreCase(nameToValidate)) {
                            isValid = true;
                            break;
                        }
                    }
                }
                
                // Only the role name is colored red if invalid
                segments.add(new Segment(roleName, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
            }
            lastEnd = m.end();
        }
        
        // Remaining text
        if (lastEnd < text.length()) {
            segments.add(new Segment(text.substring(lastEnd), ValidationStatus.PLAIN));
        }
        
        return segments;
    }

    public enum ValidationStatus {
        PLAIN,   // Normal text (black)
        VALID,   // Valid role (black)
        INVALID  // Invalid role (red)
    }

    public static class Segment {
        public final String text;
        public final ValidationStatus status;

        public Segment(String text, ValidationStatus status) {
            this.text = text;
            this.status = status;
        }
    }
}
