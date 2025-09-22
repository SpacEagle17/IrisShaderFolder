package com.spaceagle17.iris_shader_folder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderReorderSystem {
    private static ShaderReorderSystem INSTANCE;
    private static final Pattern REORDER_PATTERN = Pattern.compile("(.+)\\s*->\\s*(\\d+)");
    
    private ShaderReorderSystem() {
        // Private constructor for singleton
    }
    
    public static ShaderReorderSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderReorderSystem();
        }
        return INSTANCE;
    }
    
    public List<String> reorderShaderPacks(List<String> shaderPacks) {
        if (shaderPacks.isEmpty()) {
            return shaderPacks;
        }
        
        // Create a working copy of the input list
        List<String> result = new ArrayList<>(shaderPacks);
        
        // Special case for SpacEagle: Euphoria-Patches must be first
        if (IrisShaderFolder.isSpacEagle()) {
            String euphoriaPatches = "Euphoria-Patches";
            // Check both with and without .zip extension
            if (result.contains(euphoriaPatches)) {
                result.remove(euphoriaPatches);
                result.add(0, euphoriaPatches);
            }
            
            if (IrisShaderFolder.debugLoggingEnabled) {
                IrisShaderFolder.LOGGER.info("SpacEagle detected: Euphoria-Patches prioritized to first position");
            }
        }
        
        // If there are no reorder patterns, return the result)
        if (IrisShaderFolder.getInstance().getReorderPatterns().isEmpty()) {
            return result;
        }
        
        // Parse reorder patterns
        List<ReorderRule> rules = new ArrayList<>();
        for (String reorderPatternStr : IrisShaderFolder.getInstance().getReorderPatterns()) {
            Matcher matcher = REORDER_PATTERN.matcher(reorderPatternStr);
            if (matcher.matches()) {
                String pattern = matcher.group(1).trim();
                int position = Integer.parseInt(matcher.group(2).trim()) - 1; // Convert to 0-based index
                position = Math.max(0, position); // Ensure it's not negative
                rules.add(new ReorderRule(pattern, position));
            }
        }
        
        // Sort rules by position (lower positions first)
        rules.sort(Comparator.comparingInt(ReorderRule::getPosition));
        
        // Process each rule
        int nextAvailableIndex = IrisShaderFolder.isSpacEagle() ? 1 : 0; // Start at index 1 if Euphoria-Patches is at 0
        for (ReorderRule rule : rules) {
            // Find matching packs
            List<String> matchingPacks = new ArrayList<>();
            Iterator<String> it = result.iterator();
            while (it.hasNext()) {
                String pack = it.next();
                // If SpacEagle and this is Euphoria-Patches at index 0, skip it
                if (IrisShaderFolder.isSpacEagle() && result.indexOf(pack) == 0 && 
                    (pack.equals("Euphoria-Patches") || pack.equals("Euphoria-Patches.zip"))) {
                    continue;
                }
                
                if (ShaderFilterSystem.getInstance().matchesPattern(pack, rule.getPattern())) {
                    matchingPacks.add(pack);
                    it.remove();
                }
            }
            
            // Sort matching packs alphabetically
            Collections.sort(matchingPacks);
            
            // Determine insertion index
            int insertIndex = rule.getPosition();
            insertIndex = Math.max(insertIndex, nextAvailableIndex);
            insertIndex = Math.min(insertIndex, result.size());
            
            // Insert packs at the determined index
            for (String pack : matchingPacks) {
                result.add(insertIndex, pack);
                insertIndex++;
                nextAvailableIndex = insertIndex;
            }
        }
        
        return result;
    }
    
    private static class ReorderRule {
        private final String pattern;
        private final int position;
        
        public ReorderRule(String pattern, int position) {
            this.pattern = pattern;
            this.position = position;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public int getPosition() {
            return position;
        }
    }
}