package com.spaceagle17.iris_shader_folder.forge;

import com.spaceagle17.iris_shader_folder.forge.util.ShaderPatternUtil;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderRecolorSystem implements ConfigManager.ConfigUpdateListener {
    private static ShaderRecolorSystem INSTANCE;
    private static final Map<String, String> COLOR_MAP = new HashMap<>();

    private static final String EUPHORIA_DETECTION = "(EuphoriaPatches|Euphoria-Patches|EP_earlyDev|Complementary.* \\+ EP)";
    private static final String EUPHORIA_PATTERN = "{.*" + EUPHORIA_DETECTION + ".*}";

    // Define euphoriaRules as a class field
    private final List<ColorRule> euphoriaRules = new ArrayList<>();
    private final Map<String, String> recolorCache = new HashMap<>();
    private final Set<String> loggedRecolors = new HashSet<>();
    private static final List<String> EUPHORIA_MATCH_SAMPLES = Arrays.asList(
        "EuphoriaPatches",
        "Euphoria-Patches",
        "EP_earlyDev",
        "Complementary + EP"
    );

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
        euphoriaRules.add(new ColorRule("EuphoriaPatches_{version}-dev{version}{.*}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("+ EP_{.*}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("EuphoriaPatches_earlyDev{.*}", COLOR_MAP.get("light_purple")));
        euphoriaRules.add(new ColorRule("_0EuphoriaPatches{.*}Error{.*}Shader", COLOR_MAP.get("red")));
        euphoriaRules.add(new ColorRule("Outdated", COLOR_MAP.get("red")));

        // Register as config update listener
        ConfigManager.registerUpdateListener(this);
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
        boolean userOverridesHardcodedEuphoria = false;

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
                if (matchesHardcodedEuphoriaTargets(shaderPattern)) {
                    userOverridesHardcodedEuphoria = true;
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

        if (userOverridesHardcodedEuphoria) {
            ShaderPatternUtil.logDebug("Skipping default Euphoria recolor rules because user-defined shader_pattern overrides them");
        } else {
            addDefaultEuphoriaRules();
        }

        // Mark that we've added Euphoria rules
        euphoriaRulesAdded = !userOverridesHardcodedEuphoria;
    }

    private void addDefaultEuphoriaRules() {
        recolorRules.add(new RecolorRule(EUPHORIA_PATTERN, euphoriaRules));
        ShaderPatternUtil.logDebug("Added default recolor rule for Euphoria Patches");
    }

    private boolean matchesHardcodedEuphoriaTargets(String shaderPattern) {
        try {
            String regexPattern = ShaderPatternUtil.convertToRegex(shaderPattern);
            Pattern compiledPattern = Pattern.compile(regexPattern);

            for (String sample : EUPHORIA_MATCH_SAMPLES) {
                if (compiledPattern.matcher(sample).find()) {
                    return true;
                }
            }
        } catch (PatternSyntaxException ignored) {
            // Invalid user pattern is handled elsewhere during rule parsing.
        }

        return false;
    }

    public String recolorShaderName(String name) {
        if (recolorCache.containsKey(name)) {
            return recolorCache.get(name);
        }

        String result = name;
        boolean modified = false;
        String[] colorByIndex = new String[name.length()];

        ShaderPatternUtil.logDebug("Processing shader name: [" + name + "]");

        // Apply rules once, in configured order (user rules first, optional default last).
        ShaderPatternUtil.logDebug("--- Applying recolor rules ---");
        for (RecolorRule rule : recolorRules) {
            boolean isEuphoriaRule = rule.getShaderPattern().equals(EUPHORIA_PATTERN);

            ShaderPatternUtil.logDebug("- Checking rule with pattern: [" + rule.getShaderPattern() +
                                   (isEuphoriaRule ? "] (Euphoria rule)" : "]"));

            if (ShaderPatternUtil.matchesPattern(name, rule.getShaderPattern())) {
                ShaderPatternUtil.logDebug("  - Rule matches!");

                for (ColorRule colorRule : rule.getColorRules()) {
                    boolean ruleApplied = applyColorRule(name, colorByIndex, colorRule);

                    if (ruleApplied) {
                        modified = true;
                        ShaderPatternUtil.logDebug("  - Applied color rule [" + colorRule.getPartPattern() +
                                              " -> " + colorRule.getColorCode() + "]");
                    } else {
                        ShaderPatternUtil.logDebug("  - Color rule [" + colorRule.getPartPattern() +
                                              "] had no effect");
                    }
                }
            } else {
                ShaderPatternUtil.logDebug("  - Rule does not match");
            }
        }

        result = buildColoredResult(name, colorByIndex);

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

    private boolean applyColorRule(String input, String[] colorByIndex, ColorRule colorRule) {
        String pattern = colorRule.getPartPattern();
        String colorCode = colorRule.getColorCode();

        // Special case for {all} pattern
        if (pattern.equals("{all}")) {
            boolean applied = false;
            for (int i = 0; i < colorByIndex.length; i++) {
                colorByIndex[i] = colorCode;
                applied = true;
            }
            return applied;
        }

        try {
            // Convert pattern to regex using utility class
            String regexPattern = ShaderPatternUtil.convertToRegex(pattern);

            // Create pattern and matcher
            Pattern compiledPattern = Pattern.compile(regexPattern);
            Matcher matcher = compiledPattern.matcher(input);

            boolean applied = false;
            while (matcher.find()) {
                if (matcher.start() == matcher.end()) {
                    continue;
                }

                for (int i = matcher.start(); i < matcher.end(); i++) {
                    colorByIndex[i] = colorCode;
                }
                applied = true;
            }
            return applied;
        } catch (PatternSyntaxException e) {
            IrisShaderFolder.LOGGER.error("Invalid pattern in color rule: " + pattern, e);
            return false;
        }
    }

    private String buildColoredResult(String input, String[] colorByIndex) {
        StringBuilder result = new StringBuilder(input.length() + 16);
        String currentColorCode = null;

        for (int i = 0; i < input.length(); i++) {
            String nextColorCode = colorByIndex[i];

            if (!Objects.equals(currentColorCode, nextColorCode)) {
                if (currentColorCode != null) {
                    result.append("§r");
                }
                if (nextColorCode != null) {
                    result.append(nextColorCode);
                }
                currentColorCode = nextColorCode;
            }

            result.append(input.charAt(i));
        }

        if (currentColorCode != null) {
            result.append("§r");
        }

        return result.toString();
    }

    public void clearCache() {
        recolorCache.clear();
        loggedRecolors.clear();
    }

    // Add implementation of interface method
    @Override
    public void onConfigUpdate() {
        updateRules();
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
