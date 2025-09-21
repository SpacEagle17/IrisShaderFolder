package com.spaceagle17.iris_shader_folder;

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
        if (!ConfigManager.getInstance().isDebugLoggingEnabled()) {
            return;
        }
        
        try (FileWriter debugWriter = new FileWriter(DEBUG_FILE, append)) {
            debugWriter.write(message);
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Failed to write debug info", e);
        }
    }
    
    public void updatePatterns() {
        List<String> filterPatterns = ConfigManager.getInstance().getFilterPatterns();
        boolean debugLogging = ConfigManager.getInstance().isDebugLoggingEnabled();
        
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
                
                // Convert the pattern to regex
                String regexPattern = convertToRegex(pattern);
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
                    IrisShaderFolder.LOGGER.info("Creating pattern: " + finalPattern);
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
    
    private String convertToRegex(String pattern) {
        StringBuilder result = new StringBuilder();
        int currentPos = 0;
        
        while (currentPos < pattern.length()) {
            // Find next opening brace
            int openBrace = pattern.indexOf('{', currentPos);
            
            if (openBrace == -1) {
                // No more braces, add the rest as literal
                result.append(Pattern.quote(pattern.substring(currentPos)));
                break;
            }
            
            // Add the part before the brace as literal
            if (openBrace > currentPos) {
                result.append(Pattern.quote(pattern.substring(currentPos, openBrace)));
            }
            
            // Find the matching closing brace
            int closeBrace = findMatchingCloseBrace(pattern, openBrace);
            
            if (closeBrace == -1) {
                // No matching closing brace, treat the rest as literal
                result.append(Pattern.quote(pattern.substring(currentPos)));
                break;
            }
            
            // Extract the content inside braces
            String braceContent = pattern.substring(openBrace + 1, closeBrace);
            
            // Handle special case for {version}
            if ("version".equals(braceContent)) {
                result.append("\\d+(\\.\\d+)*");
            } else {
                // Regular expression - use as is
                result.append(braceContent);
            }
            
            // Move past the closing brace
            currentPos = closeBrace + 1;
        }
        
        return result.toString();
    }
    
    private int findMatchingCloseBrace(String text, int openBracePos) {
        int depth = 1;
        
        for (int i = openBracePos + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1; // No matching close brace
    }
    
    public boolean shouldFilterShaderPack(String packName) {
        // Check for config file changes
        if (ConfigManager.getInstance().checkForUpdates()) {
            updatePatterns();
        }
        
        for (Pattern pattern : compiledPatterns) {
            if (pattern.matcher(packName).matches()) {
                // Only log if debug logging is enabled
                if (ConfigManager.getInstance().isDebugLoggingEnabled()) {
                    IrisShaderFolder.LOGGER.info("Filtering out shader pack: " + packName);
                    writeDebug("Filtering out shader pack: " + packName + "\n", true);
                }
                return false; // Filter this pack out
            }
        }
        
        return true; // Keep this pack
    }
}