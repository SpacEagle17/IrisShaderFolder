package com.spaceagle17.iris_shader_folder;

import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderFilterSystem {
    private static ShaderFilterSystem INSTANCE;
    private static final String DEBUG_FILE = "config/iris_shader_filter_debug.txt";
    
    private List<Pattern> compiledPatterns = new ArrayList<>();
    private List<String> lastFilterPatterns = new ArrayList<>();
    private boolean lastDebugLogSetting = false;
    
    private ShaderFilterSystem() {
        updatePatterns();
    }
    
    public static ShaderFilterSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderFilterSystem();
        }
        return INSTANCE;
    }
    
    private void writeDebug(String message, boolean append) {
        if (!IrisShaderFolder.debugLoggingEnabled) {
            return;
        }
        
        try (FileWriter debugWriter = new FileWriter(DEBUG_FILE, append)) {
            debugWriter.write(message);
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Failed to write debug info", e);
        }
    }
    
    public void updatePatterns() {
        List<String> filterPatterns = IrisShaderFolder.filterPatterns;
        boolean debugLogging = IrisShaderFolder.debugLoggingEnabled;
        
        // Only recompile if the filter patterns have changed or debug logging setting changed
        if (filterPatterns.equals(lastFilterPatterns) && debugLogging == lastDebugLogSetting) {
            return;
        }
        
        // Remember current settings
        lastFilterPatterns = new ArrayList<>(filterPatterns);
        lastDebugLogSetting = debugLogging;
        
        StringBuilder debugContent = new StringBuilder();
        debugContent.append("Filter patterns (").append(filterPatterns.size()).append("):\n");
        for (String pattern : filterPatterns) {
            debugContent.append("  - '").append(pattern).append("'\n");
        }
        debugContent.append("\n");
        
        // Write initial debug info (creates new file)
        if (debugLogging) {
            writeDebug(debugContent.toString(), false);
        }
        
        compiledPatterns.clear();
        
        for (String pattern : filterPatterns) {
            pattern = pattern.trim();
            if (pattern.isEmpty() || pattern.startsWith("#")) continue;
            
            try {
                debugContent = new StringBuilder();
                if (debugLogging) {
                    debugContent.append("Processing pattern: '").append(pattern).append("'\n");
                }
                
                // Convert the pattern to regex using utility class
                String regexPattern = ShaderPatternUtil.convertToRegex(pattern);
                if (debugLogging) {
                    debugContent.append("  → Converted to regex: '").append(regexPattern).append("'\n");
                }
                
                // Add the optional .zip extension
                String finalPattern = "^" + regexPattern + "(\\.zip)?$";
                if (debugLogging) {
                    debugContent.append("  → Final pattern: '").append(finalPattern).append("'\n\n");
                    writeDebug(debugContent.toString(), true);
                }

                if (debugLogging) {
                    ShaderPatternUtil.logDebug("Creating pattern: " + finalPattern);
                }
                compiledPatterns.add(Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                String errorMsg = "Invalid filter pattern: " + pattern + " - " + e.getMessage();
                IrisShaderFolder.LOGGER.error(errorMsg);
                if (debugLogging) {
                    writeDebug("ERROR: " + errorMsg + "\n", true);
                }
            }
        }
    }
    
    public boolean shouldFilterShaderPack(String packName) {
        // Check for config file changes
        if (ConfigManager.checkForUpdates()) {
            updatePatterns();
        }
        
        for (Pattern pattern : compiledPatterns) {
            if (pattern.matcher(packName).matches()) {
                // Only log if debug logging is enabled
                if (IrisShaderFolder.debugLoggingEnabled) {
                    IrisShaderFolder.LOGGER.info("Filtering out shader pack: " + packName);
                    writeDebug("Filtering out shader pack: " + packName + "\n", true);
                }
                return false; // Filter this pack out
            }
        }
        
        return true; // Keep this pack
    }
    
    public boolean matchesPattern(String packName, String patternStr) {
        // Use the utility method instead of duplicating code
        return ShaderPatternUtil.matchesPattern(packName, patternStr);
    }
}