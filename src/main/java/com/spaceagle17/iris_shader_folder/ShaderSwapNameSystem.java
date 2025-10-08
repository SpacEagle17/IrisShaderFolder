package com.spaceagle17.iris_shader_folder;

import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderSwapNameSystem {
    private static ShaderSwapNameSystem INSTANCE;
    private final List<SwapRule> swapRules = new ArrayList<>();
    private final Map<String, SwapState> swapStates = new ConcurrentHashMap<>();
    private List<String> lastSwapNamePatterns = new ArrayList<>();
    
    private static class SwapRule {
        final Pattern pattern;
        final String originalPattern;
        final String swapName;
        final long swapTimeMs;
        final long revertTimeMs;
        
        SwapRule(String pattern, String swapName, int swapTimeSeconds, int revertTimeSeconds) {
            Pattern pattern1;
            this.originalPattern = pattern;
            
            try {
                String regexPattern = ShaderPatternUtil.convertToRegex(pattern);
                String finalPattern = "^" + regexPattern + "(\\.zip)?$";
                pattern1 = Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                IrisShaderFolder.LOGGER.error("Invalid pattern in swap rule: " + pattern + " - " + e.getMessage());
                pattern1 = Pattern.compile("^$"); // Pattern that matches nothing
            }

            this.pattern = pattern1;
            this.swapName = swapName;
            this.swapTimeMs = swapTimeSeconds * 1000L;
            this.revertTimeMs = revertTimeSeconds * 1000L;
        }
    }
    
    private static class SwapState {
        boolean isSwapped = false;
        long lastToggleTime = System.currentTimeMillis();
    }
    
    private ShaderSwapNameSystem() {
        updateSwapRules();
    }
    
    public static ShaderSwapNameSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderSwapNameSystem();
        }
        return INSTANCE;
    }
    
    public void updateSwapRules() {
        List<String> swapNamePatterns = IrisShaderFolder.getInstance().getSwapNamePatterns();
        boolean configChanged = !swapNamePatterns.equals(lastSwapNamePatterns);
        
        // Only process if config has changed
        if (!configChanged) {
            return;
        }
        
        ShaderPatternUtil.logDebug("Updating shader name swap rules");
        lastSwapNamePatterns = new ArrayList<>(swapNamePatterns);
        
        swapRules.clear();
        
        for (String rule : swapNamePatterns) {
            if (rule.trim().startsWith("#") || rule.trim().isEmpty()) continue;
            
            ShaderPatternUtil.logDebug("Processing swap rule: " + rule);
            
            // Parse the rule: pattern [->] swapName [|] swapTime [|] revertTime
            String[] parts = rule.split("\\s*\\[->\\]\\s*", 2);
            if (parts.length != 2) {
                ShaderPatternUtil.logDebug("Invalid rule format, missing [->]: " + rule);
                continue;
            }
            
            String pattern = parts[0].trim();
            String[] nameParts = parts[1].split("\\s*\\[\\|\\]\\s*");
            if (nameParts.length != 3) {
                ShaderPatternUtil.logDebug("Invalid rule format, expecting 3 parts after [->]: " + rule);
                continue;
            }
            
            String swapName = nameParts[0].trim();
            
            // Parse time values
            String swapTimeStr = nameParts[1].trim();
            String revertTimeStr = nameParts[2].trim();
            
            int swapSeconds = parseTimeValue(swapTimeStr);
            int revertSeconds = parseTimeValue(revertTimeStr);
            
            swapRules.add(new SwapRule(pattern, swapName, swapSeconds, revertSeconds));
            ShaderPatternUtil.logDebug("Added swap rule for pattern: " + pattern + 
                " → " + swapName + " (swap: " + swapSeconds + "s, revert: " + revertSeconds + "s)");
        }
    }
    
    private int parseTimeValue(String timeStr) {
        timeStr = timeStr.toLowerCase();
        if (timeStr.endsWith("s")) {
            timeStr = timeStr.substring(0, timeStr.length() - 1);
        }
        try {
            return Integer.parseInt(timeStr);
        } catch (NumberFormatException e) {
            ShaderPatternUtil.logDebug("Invalid time value: " + timeStr + ", using default of 5 seconds");
            return 5; // Default to 5 seconds
        }
    }
    
    public String swapShaderName(String originalName) {
        // Check for config updates
        if (ConfigManager.checkForUpdates()) {
            updateSwapRules();
        }
        
        if (originalName == null) {
            return originalName;
        }
        
        // Check for matching rules
        for (SwapRule rule : swapRules) {
            if (rule.pattern.matcher(originalName).matches()) {
                ShaderPatternUtil.logDebug("Found matching swap rule for: " + originalName + 
                    " with pattern: " + rule.originalPattern);
                return handleNameSwap(originalName, rule);
            }
        }
        
        return originalName;
    }
    
    private String handleNameSwap(String originalName, SwapRule rule) {
        SwapState state = swapStates.computeIfAbsent(originalName, k -> new SwapState());
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - state.lastToggleTime;
        
        // Check if it's time to toggle
        if ((state.isSwapped && elapsedTime >= rule.swapTimeMs) || 
            (!state.isSwapped && elapsedTime >= rule.revertTimeMs)) {
            state.isSwapped = !state.isSwapped;
            state.lastToggleTime = currentTime;
            
            String displayName = state.isSwapped ? rule.swapName : originalName;
            ShaderPatternUtil.logDebug("Toggled name state for: " + originalName + 
                " → now showing: " + displayName);
        }
        
        return state.isSwapped ? rule.swapName : originalName;
    }
    
    public void reset() {
        ShaderPatternUtil.logDebug("Resetting ShaderSwapNameSystem");
        swapStates.clear();
        updateSwapRules();
    }
}