package com.spaceagle17.iris_shader_folder.functionality;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.config.ConfigManager;
import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.util.*;

public class ShaderReorderSystem implements ConfigManager.ConfigUpdateListener {
    private static ShaderReorderSystem INSTANCE;

    private List<String> lastReorderPatterns = new ArrayList<>();

    private ShaderReorderSystem() {
        ConfigManager.registerUpdateListener(this);
    }

    @Override
    public void onConfigUpdate() {
        lastReorderPatterns.clear();
    }

    public static ShaderReorderSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderReorderSystem();
        }
        return INSTANCE;
    }

    private static void debugLog(String message) {
        IrisShaderFolder.debugLog("[ShaderReorderSystem]" + message);
    }

    public List<String> reorderShaderPacks(List<String> shaderPacks) {
        if (shaderPacks.isEmpty()) {
            return shaderPacks;
        }

        List<String> result = new ArrayList<>(shaderPacks);

        if (IrisShaderFolder.isSpacEagle()) {
            String euphoriaPatches = "Euphoria-Patches";
            if (result.contains(euphoriaPatches)) {
                result.remove(euphoriaPatches);
                result.add(0, euphoriaPatches);
            }

            debugLog("SpacEagle detected: Euphoria-Patches prioritized to first position");
        }

        List<String> reorderPatterns = IrisShaderFolder.getInstance().getReorderPatterns();
        if (reorderPatterns.isEmpty()) {
            return result;
        }

        List<ReorderRule> rules = new ArrayList<>();
        int rulePosition = 0;
        for (String patternLine : reorderPatterns) {
            String pattern = patternLine.trim();

            if (pattern.isEmpty() || pattern.startsWith("#")) {
                continue;
            }

            boolean forced = false;
            if (pattern.startsWith("[!]")) {
                forced = true;
                pattern = pattern.substring(3).trim();
                debugLog("Added forced reorder rule: pattern '" + pattern + "' at position " + (rulePosition + 1));
            } else {
                debugLog("Added reorder rule: pattern '" + pattern + "' at position " + (rulePosition + 1));
            }

            rules.add(new ReorderRule(pattern, rulePosition, forced));
            rulePosition++;
        }

        Set<String> alreadyMatched = new HashSet<>();

        int nextAvailableIndex = IrisShaderFolder.isSpacEagle() ? 1 : 0;
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

            Collections.sort(matchingPacks);

            int insertIndex = rule.getPosition();
            insertIndex = Math.max(insertIndex, nextAvailableIndex);
            insertIndex = Math.min(insertIndex, result.size());

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

        public String getPattern() { return pattern; }
        public int getPosition() { return position; }
        public boolean isForced() { return forced; }
    }
}
