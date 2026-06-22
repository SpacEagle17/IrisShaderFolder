package com.spaceagle17.iris_shader_folder.util;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class for shader pattern operations used across multiple systems
 */
public class ShaderPatternUtil {
    
    /**
     * Converts a user-friendly pattern string with special placeholders to a proper regex.
     * Handles {version} and {all} as special cases.
     */
    public static String convertToRegex(String pattern) {
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
            } else if ("all".equals(braceContent)) {
                result.append(".*");  // Match everything
            } else {
                // Regular expression - use as is
                result.append(braceContent);
            }
            
            // Move past the closing brace
            currentPos = closeBrace + 1;
        }
        
        return result.toString();
    }
    
    /**
     * Finds the matching closing brace for an opening brace at the given position.
     * Handles nested braces properly.
     */
    public static int findMatchingCloseBrace(String text, int openBracePos) {
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
    
    /**
     * Checks if a string matches a pattern, considering .zip extension.
     */
    public static boolean matchesPattern(String text, String patternStr) {
        if (patternStr.trim().isEmpty() || patternStr.startsWith("#")) {
            return false;
        }
        
        try {
            // Convert the pattern to regex
            String regexPattern = convertToRegex(patternStr);
            
            // Add the optional .zip extension
            String finalPattern = "^" + regexPattern + "(\\.zip)?$";
            
            // Compile and test the pattern
            Pattern pattern = Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            
            return matcher.matches();
        } catch (PatternSyntaxException e) {
            String errorMsg = "Invalid pattern: " + patternStr + " - " + e.getMessage();
            IrisShaderFolder.LOGGER.error(errorMsg);
            if (IrisShaderFolder.debugLoggingEnabled) {
                IrisShaderFolder.LOGGER.debug("ERROR: " + errorMsg);
            }
            return false;
        }
    }
    
    /**
     * Helper method to log debug information conditionally.
     */
    public static void logDebug(String message) {
        if (IrisShaderFolder.debugLoggingEnabled) {
            IrisShaderFolder.LOGGER.info(message);
        }
    }
}