package com.example.exceljson.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceGroupValidator {

    private static final Pattern VGROUP_PATTERN = Pattern.compile("(?i)((?:VGroup|Group):\\s*)([^,;\\n]+)");

    public static List<List<Segment>> parseAndValidateMultiLine(String text, Set<String> loadedVoiceGroups) {
        List<List<Segment>> lineSegments = new ArrayList<>();
        if (text == null) {
            return lineSegments;
        }

        String[] lines = text.split("\\n");
        for (String line : lines) {
            lineSegments.add(parseAndValidate(line, loadedVoiceGroups));
        }
        return lineSegments;
    }
    
    public static List<Segment> parseAndValidate(String text, Set<String> loadedVoiceGroups) {
        List<Segment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        Matcher m = VGROUP_PATTERN.matcher(text);
        
        // Check if VGROUP keyword pattern exists in the text
        if (m.find()) {
            // VGROUP keyword found - use existing logic
            m.reset(); // Reset matcher to start from beginning
            int lastEnd = 0;
            
            while (m.find()) {
                // Text before the match
                if (m.start() > lastEnd) {
                    segments.add(new Segment(text.substring(lastEnd, m.start()), ValidationStatus.PLAIN));
                }
                
                // The prefix (e.g. "VGroup: " or "Group: ")
                String prefix = m.group(1);
                segments.add(new Segment(prefix, ValidationStatus.PLAIN));
                
                // The group name - this is what we validate
                String groupName = m.group(2).trim();
                
                // Clean group name for validation (remove trailing special chars like #)
                String nameToValidate = groupName.replaceAll("[^a-zA-Z0-9_\\-]+$", "").trim();
                
                // Check if this specific group name exists in loaded groups (case-insensitive)
                boolean isValid = false;
                if (!nameToValidate.isEmpty()) {
                    for (String validGroup : loadedVoiceGroups) {
                        if (validGroup.equalsIgnoreCase(nameToValidate)) {
                            isValid = true;
                            break;
                        }
                    }
                }
                
                // Only the group name is colored red if invalid, not the entire text
                segments.add(new Segment(groupName, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
                lastEnd = m.end();
            }
            
            // Remaining text
            if (lastEnd < text.length()) {
                segments.add(new Segment(text.substring(lastEnd), ValidationStatus.PLAIN));
            }
        } else {
            // No VGROUP keyword - validate comma/semicolon-separated values individually
            // Only validate if there are delimiters - plain text without delimiters should remain PLAIN
            if (text.indexOf(',') < 0 && text.indexOf(';') < 0) {
                segments.add(new Segment(text, ValidationStatus.PLAIN));
                return segments;
            }
            
            // Use regex to split while preserving delimiters and spaces
            int lastIndex = 0;
            Pattern delimiterPattern = Pattern.compile("[,;]");
            Matcher delimiterMatcher = delimiterPattern.matcher(text);
            
            while (delimiterMatcher.find()) {
                // Get the text before the delimiter
                String beforeDelimiter = text.substring(lastIndex, delimiterMatcher.start());
                String trimmed = beforeDelimiter.trim();
                
                // Add leading spaces if any
                int leadingSpaces = beforeDelimiter.indexOf(trimmed);
                if (leadingSpaces > 0) {
                    segments.add(new Segment(beforeDelimiter.substring(0, leadingSpaces), ValidationStatus.PLAIN));
                }
                
                // Validate the trimmed text
                if (!trimmed.isEmpty()) {
                    boolean isValid = false;
                    for (String validGroup : loadedVoiceGroups) {
                        if (validGroup.equalsIgnoreCase(trimmed)) {
                            isValid = true;
                            break;
                        }
                    }
                    segments.add(new Segment(trimmed, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
                }
                
                // Add trailing spaces if any
                int trailingSpaces = beforeDelimiter.length() - leadingSpaces - trimmed.length();
                if (trailingSpaces > 0) {
                    segments.add(new Segment(beforeDelimiter.substring(leadingSpaces + trimmed.length()), ValidationStatus.PLAIN));
                }
                
                // Add the delimiter
                segments.add(new Segment(delimiterMatcher.group(), ValidationStatus.PLAIN));
                lastIndex = delimiterMatcher.end();
            }
            
            // Handle the last segment (after the last delimiter or the whole text if no delimiters)
            if (lastIndex < text.length()) {
                String remaining = text.substring(lastIndex);
                String trimmed = remaining.trim();
                
                // Add leading spaces if any
                int leadingSpaces = remaining.indexOf(trimmed);
                if (leadingSpaces > 0) {
                    segments.add(new Segment(remaining.substring(0, leadingSpaces), ValidationStatus.PLAIN));
                }
                
                // Validate the trimmed text
                if (!trimmed.isEmpty()) {
                    boolean isValid = false;
                    for (String validGroup : loadedVoiceGroups) {
                        if (validGroup.equalsIgnoreCase(trimmed)) {
                            isValid = true;
                            break;
                        }
                    }
                    segments.add(new Segment(trimmed, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
                }
                
                // Add trailing spaces if any
                int trailingSpaces = remaining.length() - leadingSpaces - trimmed.length();
                if (trailingSpaces > 0) {
                    segments.add(new Segment(remaining.substring(leadingSpaces + trimmed.length()), ValidationStatus.PLAIN));
                }
            }
        }
        
        return segments;
    }

    /**
     * Multi-line version of parseAndValidateAlways - validates each line independently.
     * Always validates group names against loaded groups, even without VGroup/Group: keyword.
     * Used for columns like "No Caregiver Group" where validation should always run.
     * 
     * @param text The text to validate (can contain multiple lines)
     * @param loadedVoiceGroups Set of valid group names
     * @return List of segment lists, one per line
     */
    public static List<List<Segment>> parseAndValidateAlwaysMultiLine(String text, Set<String> loadedVoiceGroups) {
        List<List<Segment>> lineSegments = new ArrayList<>();
        if (text == null) {
            return lineSegments;
        }

        String[] lines = text.split("\\n");
        for (String line : lines) {
            lineSegments.add(parseAndValidateAlways(line, loadedVoiceGroups));
        }
        return lineSegments;
    }
    
    /**
     * Parses and validates group names against loaded voice groups.
     * Always validates, even without VGroup/Group: keyword prefix.
     * 
     * If text has VGroup:/Group: pattern, extracts and validates the group name after the prefix.
     * If text has no pattern, validates the entire trimmed text as a group name.
     * 
     * @param text The text to validate
     * @param loadedVoiceGroups Set of valid group names
     * @return List of segments with validation status
     */
    public static List<Segment> parseAndValidateAlways(String text, Set<String> loadedVoiceGroups) {
        List<Segment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        Matcher m = VGROUP_PATTERN.matcher(text);
        
        // Check if VGROUP keyword pattern exists in the text
        if (m.find()) {
            // VGROUP keyword found - use existing logic (same as parseAndValidate)
            m.reset(); // Reset matcher to start from beginning
            int lastEnd = 0;
            
            while (m.find()) {
                // Text before the match
                if (m.start() > lastEnd) {
                    segments.add(new Segment(text.substring(lastEnd, m.start()), ValidationStatus.PLAIN));
                }
                
                // The prefix (e.g. "VGroup: " or "Group: ")
                String prefix = m.group(1);
                segments.add(new Segment(prefix, ValidationStatus.PLAIN));
                
                // The group name - this is what we validate
                String groupName = m.group(2).trim();
                
                // Clean group name for validation (remove trailing special chars like #)
                String nameToValidate = groupName.replaceAll("[^a-zA-Z0-9_\\-]+$", "").trim();
                
                // Check if this specific group name exists in loaded groups (case-insensitive)
                boolean isValid = isGroupValid(nameToValidate, loadedVoiceGroups);
                
                // Only the group name is colored red if invalid, not the entire text
                segments.add(new Segment(groupName, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
                lastEnd = m.end();
            }
            
            // Remaining text
            if (lastEnd < text.length()) {
                segments.add(new Segment(text.substring(lastEnd), ValidationStatus.PLAIN));
            }
        } else {
            // No VGROUP keyword - validate the text as a single group name or as comma/semicolon-separated values
            if (text.indexOf(',') >= 0 || text.indexOf(';') >= 0) {
                // Has delimiters - validate each separated value (same as existing parseAndValidate logic)
                return parseAndValidate(text, loadedVoiceGroups);
            } else {
                // Single value without delimiters - validate the entire text as a group name
                String trimmed = text.trim();
                if (trimmed.isEmpty()) {
                    segments.add(new Segment(text, ValidationStatus.PLAIN));
                    return segments;
                }
                
                // Preserve leading/trailing whitespace as PLAIN segments
                int leadingSpaces = text.indexOf(trimmed);
                if (leadingSpaces > 0) {
                    segments.add(new Segment(text.substring(0, leadingSpaces), ValidationStatus.PLAIN));
                }
                
                // Validate the trimmed group name
                boolean isValid = isGroupValid(trimmed, loadedVoiceGroups);
                segments.add(new Segment(trimmed, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
                
                // Trailing whitespace
                int trailingStart = leadingSpaces + trimmed.length();
                if (trailingStart < text.length()) {
                    segments.add(new Segment(text.substring(trailingStart), ValidationStatus.PLAIN));
                }
            }
        }
        
        return segments;
    }
    
    /**
     * Helper method to check if a group name is valid (exists in loaded groups, case-insensitive).
     */
    private static boolean isGroupValid(String groupName, Set<String> loadedVoiceGroups) {
        if (groupName == null || groupName.isEmpty()) {
            return false;
        }
        for (String validGroup : loadedVoiceGroups) {
            if (validGroup.equalsIgnoreCase(groupName)) {
                return true;
            }
        }
        return false;
    }

    public enum ValidationStatus {
        PLAIN,   // Normal text (black)
        VALID,   // Valid group (black/green?) -> Requirement says Black
        INVALID  // Invalid group (red)
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
