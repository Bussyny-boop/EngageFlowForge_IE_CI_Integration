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
    
    private static List<Segment> parseAndValidate(String text, Set<String> loadedVoiceGroups) {
        List<Segment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        Matcher m = VGROUP_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (m.find()) {
            // Text before the match
            if (m.start() > lastEnd) {
                segments.add(new Segment(text.substring(lastEnd, m.start()), ValidationStatus.PLAIN));
            }
            
            // The prefix (e.g. "VGroup: ")
            String prefix = m.group(1);
            segments.add(new Segment(prefix, ValidationStatus.PLAIN));
            
            // The group name
            String groupName = m.group(2);
            
            // Clean group name for validation (remove trailing special chars like #)
            String nameToValidate = groupName.replaceAll("[^a-zA-Z0-9_\\-]+$", "");
            
            boolean isValid = false;
            for (String validGroup : loadedVoiceGroups) {
                if (validGroup.equalsIgnoreCase(nameToValidate) || validGroup.equalsIgnoreCase(groupName)) {
                    isValid = true;
                    break;
                }
            }
            
            segments.add(new Segment(groupName, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
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
