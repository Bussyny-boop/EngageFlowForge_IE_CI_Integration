package com.example.exceljson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for transforming alert type names based on configuration.
 * Reads mappings from config.yml and applies transformations to shorten long alert names.
 */
public class AlertNameTransformer {
    
    private final Map<String, String> transformations = new LinkedHashMap<>();
    
    /**
     * Load alert name transformations from config.yml
     */
    public AlertNameTransformer() {
        loadTransformations();
    }
    
    /**
     * Transform an alert name using the configured mappings.
     * Returns the transformed name if a mapping exists, otherwise returns the original name.
     * 
     * @param alertName The original alert name
     * @return The transformed alert name or the original if no mapping exists
     */
    public String transform(String alertName) {
        if (alertName == null || alertName.trim().isEmpty()) {
            return alertName;
        }
        
        // Try exact match first (case-sensitive)
        String transformed = transformations.get(alertName);
        if (transformed != null) {
            return transformed;
        }
        
        // Try case-insensitive match
        for (Map.Entry<String, String> entry : transformations.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(alertName)) {
                return entry.getValue();
            }
        }
        
        // No transformation found, return original
        return alertName;
    }
    
    /**
     * Load transformations from config.yml
     */
    private void loadTransformations() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (is == null) {
                System.err.println("Warning: config.yml not found, alert name transformations disabled");
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            @SuppressWarnings("unchecked")
            Map<String, Object> config = mapper.readValue(is, Map.class);
            
            // Extract alertNameTransformations section
            @SuppressWarnings("unchecked")
            Map<String, String> alertTransforms = (Map<String, String>) config.get("alertNameTransformations");
            
            if (alertTransforms != null) {
                transformations.putAll(alertTransforms);
                System.out.println("✅ Loaded " + transformations.size() + " alert name transformations");
            } else {
                System.out.println("ℹ️ No alert name transformations defined in config.yml");
            }
            
        } catch (IOException e) {
            System.err.println("Warning: Failed to load alert name transformations from config.yml: " + e.getMessage());
        }
    }
    
    /**
     * Get all configured transformations for debugging/display purposes
     */
    public Map<String, String> getTransformations() {
        return new LinkedHashMap<>(transformations);
    }
}
