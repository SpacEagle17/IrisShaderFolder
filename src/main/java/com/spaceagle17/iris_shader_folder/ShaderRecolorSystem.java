package com.spaceagle17.iris_shader_folder;

import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderRecolorSystem {
    private static ShaderRecolorSystem INSTANCE;
    private static final Map<String, String> COLOR_MAP = new HashMap<>();
    
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
    }
    
    private ShaderRecolorSystem() {
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
        boolean hasUserEuphoriaRules = false;
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
                
                // Check if this is a user-defined rule for Euphoria shaders
                if (shaderPattern.contains("EuphoriaPatches") || shaderPattern.contains("Euphoria-Patches")) {
                    hasUserEuphoriaRules = true;
                }
                
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
        
        // Always add default Euphoria rules if no user-defined rules exist for them
        if (!hasUserEuphoriaRules) {
            addDefaultEuphoriaRules();
        }
        
        // Mark that we've added Euphoria rules
        euphoriaRulesAdded = true;
    }
    
    private void addDefaultEuphoriaRules() {
        String combinedPattern = "{.*(EuphoriaPatches|Euphoria-Patches|EP_earlyDev|Complementary.* \\+ EP).*}";
        
        List<ColorRule> euphoriaRules = new ArrayList<>();
        euphoriaRules.add(new ColorRule("+ EuphoriaPatches_{version}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("Euphoria-Patches{.*}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("EP_{.*}", COLOR_MAP.get("light_purple")));

        
        recolorRules.add(new RecolorRule(combinedPattern, euphoriaRules));   
        ShaderPatternUtil.logDebug("Added default recolor rule for Euphoria Patches");
    }
    
    public String recolorShaderName(String name) {
        // Always check for updates to ensure Euphoria rules are added
        if (ConfigManager.checkForUpdates() || !euphoriaRulesAdded) {
            updateRules();
        }
        
        // Don't return early if recolorRules is empty - we'll ensure it's not empty
        // by always adding Euphoria rules in updateRules()
        
        if (recolorCache.containsKey(name)) {
            return recolorCache.get(name);
        }
        
        String result = name;
        boolean modified = false;
        
        for (RecolorRule rule : recolorRules) {
            // Check if shader name matches the pattern
            if (ShaderPatternUtil.matchesPattern(name, rule.getShaderPattern())) {
                // Apply each color rule
                for (ColorRule colorRule : rule.getColorRules()) {
                    String before = result;
                    result = applyColorRule(result, colorRule);
                    if (!before.equals(result)) {
                        modified = true;
                    }
                }
            }
        }
        
        // Store in cache
        recolorCache.put(name, result);
        
        // Log only if modified and we haven't logged this specific recoloring before
        if (modified && IrisShaderFolder.debugLoggingEnabled) {
            String cacheKey = name + " -> " + result;
            if (!loggedRecolors.contains(cacheKey)) {
                IrisShaderFolder.LOGGER.info("Recolored shader name: Before [" + name + "] -> After [" + result + "]");
                loggedRecolors.add(cacheKey);
            }
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