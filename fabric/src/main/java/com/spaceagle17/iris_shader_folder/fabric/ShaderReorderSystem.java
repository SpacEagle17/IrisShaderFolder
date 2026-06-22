package com.spaceagle17.iris_shader_folder.fabric;

import com.spaceagle17.iris_shader_folder.fabric.util.ShaderPatternUtil;
import java.util.*;

public class ShaderReorderSystem implements ConfigManager.ConfigUpdateListener {
    private static ShaderReorderSystem INSTANCE;

    private List<String> lastReorderPatterns = new ArrayList<>();

    private ShaderReorderSystem() {
        ConfigManager.registerUpdateListener(this);
    }

    @Override
    public void onConfigUpdate() {
        // Clear any cached data or pattern compilations
        lastReorderPatterns.clear();
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
        int rulePosition = 0;
        for (String patternLine : reorderPatterns) {
            String pattern = patternLine.trim();

            // Skip empty lines and comments
            if (pattern.isEmpty() || pattern.startsWith("#")) {
                continue;
            }

            // Check for [!] prefix (forced - don't skip already matched)
            boolean forced = false;
            if (pattern.startsWith("[!]")) {
                forced = true;
                pattern = pattern.substring(3).trim(); // Remove [!] prefix
                ShaderPatternUtil.logDebug("Added forced reorder rule: pattern '" + pattern + "' at position " + (rulePosition + 1));
            } else {
                ShaderPatternUtil.logDebug("Added reorder rule: pattern '" + pattern + "' at position " + (rulePosition + 1));
            }

            rules.add(new ReorderRule(pattern, rulePosition, forced));
            rulePosition++;
        }

        Set<String> alreadyMatched = new HashSet<>();

        // Process each rule in order
        int nextAvailableIndex = IrisShaderFolder.isSpacEagle() ? 1 : 0; // Start at index 1 if Euphoria-Patches is at 0
        for (ReorderRule rule : rules) {
            List<String> matchingPacks = new ArrayList<>();
            Iterator<String> it = result.iterator();
            while (it.hasNext()) {
                String pack = it.next();

                if (IrisShaderFolder.isSpacEagle() && result.indexOf(pack) == 0 &&
                    (pack.equals("Euphoria-Patches") || pack.equals("Euphoria-Patches.zip"))) {
                    continue;
                }

                if (alreadyMatched.contains(pack) && !rule.isForced()) {
                    continue;
                }

                if (ShaderPatternUtil.matchesPattern(pack, rule.getPattern())) {
                    matchingPacks.add(pack);
                    alreadyMatched.add(pack);
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
        private final boolean forced;

        public ReorderRule(String pattern, int position, boolean forced) {
            this.pattern = pattern;
            this.position = position;
            this.forced = forced;
        }

        public String getPattern() {
            return pattern;
        }

        public int getPosition() {
            return position;
        }

        public boolean isForced() {
            return forced;
        }
    }
}
