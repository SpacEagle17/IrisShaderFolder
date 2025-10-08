package com.spaceagle17.iris_shader_folder;

import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderRecolorSystem {
    private static ShaderRecolorSystem INSTANCE;
    private static final Map<String, String> COLOR_MAP = new HashMap<>();
        
    private static final String EUPHORIA_DETECTION = "(EuphoriaPatches|Euphoria-Patches|EP_earlyDev|Complementary.* \\+ EP)";
    private static final String EUPHORIA_PATTERN = "{.*" + EUPHORIA_DETECTION + ".*}";

    // Define euphoriaRules as a class field
    private final List<ColorRule> euphoriaRules = new ArrayList<>();
    private final Map<String, String> recolorCache = new HashMap<>();
    private final Set<String> loggedRecolors = new HashSet<>();
    
    private List<RecolorRule> recolorRules = new ArrayList<>();
    private List<String> lastRecolorPatterns = new ArrayList<>();
    private boolean euphoriaRulesAdded = false; // Track if Euphoria rules have been added

    static {
        COLOR_MAP.put("black", "§0");
        COLOR_MAP.put("dark_blue", "§1");
        COLOR_MAP.put("dark_green", "§2");
        COLOR_MAP.put("dark_aqua", "§3");
        COLOR_MAP.put("dark_red", "§4");
        COLOR_MAP.put("dark_purple", "§5");
        COLOR_MAP.put("gold", "§6");
        COLOR_MAP.put("gray", "§7");
        COLOR_MAP.put("dark_gray", "§8");
        COLOR_MAP.put("blue", "§9");
        COLOR_MAP.put("green", "§a");
        COLOR_MAP.put("aqua", "§b");
        COLOR_MAP.put("red", "§c");
        COLOR_MAP.put("light_purple", "§d");
        COLOR_MAP.put("yellow", "§e");
        COLOR_MAP.put("white", "§f");
        COLOR_MAP.put("bold", "§l");
        COLOR_MAP.put("italic", "§o");
        COLOR_MAP.put("underline", "§n");
        COLOR_MAP.put("strikethrough", "§m");
        COLOR_MAP.put("reset", "§r");
        COLOR_MAP.put("obfuscated", "§k");
    }
    
    private ShaderRecolorSystem() {
        // Initialize euphoriaRules in the constructor
        euphoriaRules.add(new ColorRule("+ EuphoriaPatches_{version}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("Euphoria-Patches{.*}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("+ EP_{.*}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("EuphoriaPatches_earlyDev{.*}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("_0EuphoriaPatches Error Shader", COLOR_MAP.get("red")));
        euphoriaRules.add(new ColorRule("Outdated", COLOR_MAP.get("red")));
        
        updateRules();
    }
    
    public static ShaderRecolorSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderRecolorSystem();
        }
        return INSTANCE;
    }
    
    public void updateRules() {
        List<String> recolorPatterns = IrisShaderFolder.getInstance().getRecolorPatterns();
        boolean configChanged = !recolorPatterns.equals(lastRecolorPatterns);
        
        // Always process at least once to add Euphoria rules, or if config changed
        if (!configChanged && euphoriaRulesAdded) {
            return;
        }
        
        lastRecolorPatterns = new ArrayList<>(recolorPatterns);
        
        recolorRules.clear();
        recolorCache.clear();
        loggedRecolors.clear();
        euphoriaRulesAdded = false;
        
        // Process user-defined rules
        for (String rule : recolorPatterns) {
            rule = rule.trim();
            if (rule.isEmpty() || rule.startsWith("#")) continue;
            
            try {
                // Split by [|] to separate shader pattern from color rules
                String[] parts = rule.split("\\s*\\[\\|\\]\\s*");
                
                if (parts.length < 2) {
                    IrisShaderFolder.LOGGER.error("Invalid recolor rule format: {}", rule);
                    continue;
                }
                
                String shaderPattern = parts[0].trim();
                
                // Process color rules (part_pattern [->] color_name)
                List<ColorRule> colorRulesList = new ArrayList<>();
                
                for (int i = 1; i < parts.length; i++) {
                    String[] colorParts = parts[i].split("\\s*\\[->\\]\\s*");
                    if (colorParts.length != 2) {
                        IrisShaderFolder.LOGGER.error("Invalid color rule format: {}", parts[i]);
                        continue;
                    }
                    
                    String partPattern = colorParts[0].trim();
                    String colorName = colorParts[1].trim();
                    
                    // Get color code from name or use as is if it starts with §
                    String colorCode = colorName.startsWith("§") 
                        ? colorName 
                        : COLOR_MAP.getOrDefault(colorName.toLowerCase(), "§f");
                    
                    colorRulesList.add(new ColorRule(partPattern, colorCode));
                }
                
                if (!colorRulesList.isEmpty()) {
                    recolorRules.add(new RecolorRule(shaderPattern, colorRulesList));
                    ShaderPatternUtil.logDebug("Added recolor rule for pattern: " + shaderPattern + 
                        " with " + colorRulesList.size() + " color rules");
                }
            } catch (Exception e) {
                IrisShaderFolder.LOGGER.error("Error parsing recolor rule: " + rule, e);
            }
        }
        
        addDefaultEuphoriaRules();
        
        // Mark that we've added Euphoria rules
        euphoriaRulesAdded = true;
    }
    
    private void addDefaultEuphoriaRules() {
        recolorRules.add(new RecolorRule(EUPHORIA_PATTERN, euphoriaRules));   
        ShaderPatternUtil.logDebug("Added default recolor rule for Euphoria Patches");
    }

    // Add this special method to handle pre-colored content in the second pass
    private String applyEuphoriaColorRules(String input) {
        ShaderPatternUtil.logDebug("Applying Euphoria color rules to pre-colored content: " + input);
        
        // Strip color codes for pattern matching
        String strippedInput = input.replaceAll("§[0-9a-fklmnor]", "");
        ShaderPatternUtil.logDebug("Stripped input: " + strippedInput);
        
        // Process all euphoria rules, don't hardcode specific ones
        for (ColorRule rule : euphoriaRules) {
            String patternStr = rule.getPartPattern();
            String colorCode = rule.getColorCode();
            
            // Convert pattern to a proper regex
            String regex;
            if (patternStr.contains("{version}") || patternStr.contains("{.*}")) {
                regex = patternStr
                    .replace("+ ", "\\+ ")  // Escape plus sign
                    .replace("{version}", "[0-9.]+")
                    .replace("{.*}", ".*");
            } else {
                // For exact match patterns (like "Outdated")
                regex = Pattern.quote(patternStr);
            }
            
            // Try to find this pattern in the stripped input
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(strippedInput);
            
            if (matcher.find()) {
                String match = matcher.group();
                ShaderPatternUtil.logDebug("Found match for rule [" + patternStr + "]: " + match);
                
                // Find the position in the original string
                int startIndex = strippedInput.indexOf(match);
                if (startIndex >= 0) {
                    int coloredStartIndex = findColoredPosition(input, strippedInput, startIndex);
                    int coloredEndIndex = findColoredPosition(input, strippedInput, startIndex + match.length());
                    
                    if (coloredStartIndex >= 0 && coloredEndIndex > coloredStartIndex) {
                        String before = input.substring(0, coloredStartIndex);
                        String after = input.substring(coloredEndIndex);
                        
                        // Check if this section already has color codes
                        String targetSection = input.substring(coloredStartIndex, coloredEndIndex);
                        
                        // If section already starts with a color code, remove it
                        if (targetSection.startsWith("§") && targetSection.length() >= 2) {
                            // Remove the existing color code(s)
                            targetSection = targetSection.replaceAll("^(§[0-9a-fklmnor])+", "");
                            
                            // Apply the new color
                            String coloredResult = before + colorCode + targetSection + "§r" + after;
                            
                            ShaderPatternUtil.logDebug("Applied rule [" + patternStr + "] with color [" + colorCode + "] (replacing existing color):");
                            ShaderPatternUtil.logDebug("  * Before: " + input);
                            ShaderPatternUtil.logDebug("  * After:  " + coloredResult);
                            
                            input = coloredResult;
                        } else {
                            // No existing color, add new one
                            String coloredResult = before + colorCode + match + "§r" + after;
                            
                            ShaderPatternUtil.logDebug("Applied rule [" + patternStr + "] with color [" + colorCode + "]:");
                            ShaderPatternUtil.logDebug("  * Before: " + input);
                            ShaderPatternUtil.logDebug("  * After:  " + coloredResult);
                            
                            input = coloredResult;
                        }
                    }
                }
            }
        }
        
        // Clean up any remaining duplicate color codes
        input = input.replaceAll("(§[0-9a-fklmnor])\\1+", "$1");
        
        return input;
    }

    // Helper method to find position in colored string corresponding to position in stripped string
    private int findColoredPosition(String colored, String stripped, int strippedPos) {
        if (strippedPos == 0) return 0;
        if (strippedPos >= stripped.length()) return colored.length();
        
        int coloredPos = 0;
        int strippedIndex = 0;
        
        while (strippedIndex < strippedPos && coloredPos < colored.length()) {
            // Skip color codes
            if (colored.charAt(coloredPos) == '§' && coloredPos + 1 < colored.length()) {
                coloredPos += 2; // Skip § and the next character
            } else {
                coloredPos++;
                strippedIndex++;
            }
        }
        
        return coloredPos;
    }
    
    public String recolorShaderName(String name) {
        // Always check for updates to ensure Euphoria rules are added
        if (ConfigManager.checkForUpdates() || !euphoriaRulesAdded) {
            updateRules();
        }
        
        if (recolorCache.containsKey(name)) {
            return recolorCache.get(name);
        }
        
        String result = name;
        boolean modified = false;
        boolean hasEuphoriaContent = Pattern.compile(EUPHORIA_DETECTION).matcher(name).find();
        
        ShaderPatternUtil.logDebug("Processing shader name: [" + name + "]");
        if (hasEuphoriaContent) {
            ShaderPatternUtil.logDebug("- Contains Euphoria content (matches pattern: " + EUPHORIA_DETECTION + ")");
        }
        
        // First apply ALL rules as normal
        ShaderPatternUtil.logDebug("--- FIRST PASS: Applying all rules ---");
        for (RecolorRule rule : recolorRules) {
            boolean isEuphoriaRule = rule.getShaderPattern().equals(EUPHORIA_PATTERN);
            
            ShaderPatternUtil.logDebug("- Checking rule with pattern: [" + rule.getShaderPattern() + 
                                   (isEuphoriaRule ? "] (Euphoria rule)" : "]"));
            
            if (ShaderPatternUtil.matchesPattern(name, rule.getShaderPattern())) {
                ShaderPatternUtil.logDebug("  - Rule matches!");
                
                for (ColorRule colorRule : rule.getColorRules()) {
                    String before = result;
                    result = applyColorRule(result, colorRule);
                    
                    if (!before.equals(result)) {
                        modified = true;
                        ShaderPatternUtil.logDebug("  - Applied color rule [" + colorRule.getPartPattern() + 
                                              " -> " + colorRule.getColorCode() + "]");
                        ShaderPatternUtil.logDebug("    * Before: [" + before + "]");
                        ShaderPatternUtil.logDebug("    * After:  [" + result + "]");
                    } else {
                        ShaderPatternUtil.logDebug("  - Color rule [" + colorRule.getPartPattern() + 
                                              "] had no effect");
                    }
                }
            } else {
                ShaderPatternUtil.logDebug("  - Rule does not match");
            }
        }
        
        // Then re-apply ONLY the Euphoria rules if needed to ensure they take precedence
        if (hasEuphoriaContent) {
            ShaderPatternUtil.logDebug("--- SECOND PASS: Re-applying Euphoria rules ---");
            
            // Use special method for pre-colored content
            String before = result;
            result = applyEuphoriaColorRules(result);
            
            if (!before.equals(result)) {
                modified = true;
                ShaderPatternUtil.logDebug("Euphoria rules successfully applied in second pass");
            } else {
                ShaderPatternUtil.logDebug("No changes from Euphoria rules in second pass");
            }
        }
        
        // Store in cache
        recolorCache.put(name, result);
        
        // Log final result
        if (modified) {
            ShaderPatternUtil.logDebug("=== FINAL RESULT ===");
            ShaderPatternUtil.logDebug("- Original: [" + name + "]");
            ShaderPatternUtil.logDebug("- Recolored: [" + result + "]");
        }
        
        return result;
    }
    
    private String applyColorRule(String input, ColorRule colorRule) {
        String pattern = colorRule.getPartPattern();
        String colorCode = colorRule.getColorCode();
        
        // Special case for {all} pattern
        if (pattern.equals("{all}")) {
            return colorCode + input + "§r";
        }
        
        try {
            // Convert pattern to regex using utility class
            String regexPattern = ShaderPatternUtil.convertToRegex(pattern);
            
            // Create pattern and matcher
            Pattern compiledPattern = Pattern.compile(regexPattern);
            Matcher matcher = compiledPattern.matcher(input);
            
            // Replace matching parts with colored versions
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String replacement = colorCode + matcher.group() + "§r";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            
            return result.toString();
        } catch (PatternSyntaxException e) {
            IrisShaderFolder.LOGGER.error("Invalid pattern in color rule: " + pattern, e);
            return input;
        }
    }
    
    public void clearCache() {
        recolorCache.clear();
        loggedRecolors.clear();
    }
    
    private static class RecolorRule {
        private final String shaderPattern;
        private final List<ColorRule> colorRules;
        
        public RecolorRule(String shaderPattern, List<ColorRule> colorRules) {
            this.shaderPattern = shaderPattern;
            this.colorRules = colorRules;
        }
        
        public String getShaderPattern() {
            return shaderPattern;
        }
        
        public List<ColorRule> getColorRules() {
            return colorRules;
        }
    }
    
    private static class ColorRule {
        private final String partPattern;
        private final String colorCode;
        
        public ColorRule(String partPattern, String colorCode) {
            this.partPattern = partPattern;
            this.colorCode = colorCode;
        }
        
        public String getPartPattern() {
            return partPattern;
        }
        
        public String getColorCode() {
            return colorCode;
        }
    }
}