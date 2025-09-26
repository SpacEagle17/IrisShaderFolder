package com.spaceagle17.iris_shader_folder;

import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.util.*;

public class ShaderReorderSystem {
    private static ShaderReorderSystem INSTANCE;
    
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
            
            ShaderPatternUtil.logDebug("SpacEagle detected: Euphoria-Patches prioritized to first position");
        }
        
        // If there are no reorder patterns, return the result
        List<String> reorderPatterns = IrisShaderFolder.getInstance().getReorderPatterns();
        if (reorderPatterns.isEmpty()) {
            return result;
        }
        
        // Create rules based on line order
        List<ReorderRule> rules = new ArrayList<>();
        for (int i = 0; i < reorderPatterns.size(); i++) {
            String pattern = reorderPatterns.get(i).trim();
            // Position is determined by line index (convert to 0-based)
            int position = i;
            rules.add(new ReorderRule(pattern, position));
            
            ShaderPatternUtil.logDebug("Added reorder rule: pattern '" + pattern + "' at position " + (position + 1));
        }
        
        // Process each rule in order
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
                
                if (ShaderPatternUtil.matchesPattern(pack, rule.getPattern())) {
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