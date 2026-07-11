package com.spaceagle17.iris_shader_folder.functionality;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.config.ConfigManager;
import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderFilterSystem implements ConfigManager.ConfigUpdateListener {
    private static ShaderFilterSystem INSTANCE;

    private List<Pattern> compiledPatterns = new ArrayList<>();
    private List<String> lastFilterPatterns = new ArrayList<>();

    private ShaderFilterSystem() {
        ConfigManager.registerUpdateListener(this);
        updatePatterns();
    }

    private static void debugLog(String message) {
        IrisShaderFolder.debugLog("[ShaderFilterSystem]" + message);
    }

    public static ShaderFilterSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderFilterSystem();
        }
        return INSTANCE;
    }

    @Override
    public void onConfigUpdate() {
        updatePatterns();
    }

    public void updatePatterns() {
        List<String> filterPatterns = IrisShaderFolder.getInstance().getFilterPatterns();

        if (filterPatterns.equals(lastFilterPatterns)) {
            return;
        }

        lastFilterPatterns = new ArrayList<>(filterPatterns);

        debugLog("Filter patterns (" + filterPatterns.size() + "):");
        for (String pattern : filterPatterns) {
            debugLog("  - '" + pattern + "'");
        }

        compiledPatterns.clear();

        for (String pattern : filterPatterns) {
            pattern = pattern.trim();
            if (pattern.isEmpty() || pattern.startsWith("#")) continue;

            try {
                debugLog("Processing pattern: '" + pattern + "'");

                String regexPattern = ShaderPatternUtil.convertToRegex(pattern);
                debugLog("  -> Converted to regex: '" + regexPattern + "'");

                String finalPattern = "^" + regexPattern + "(\\.zip)?$";
                debugLog("  -> Final pattern: '" + finalPattern + "'");

                compiledPatterns.add(Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                IrisShaderFolder.log(3, "Invalid filter pattern: " + pattern + " - " + e.getMessage());
            }
        }
    }

    public boolean shouldFilterShaderPack(String packName) {
        for (Pattern pattern : compiledPatterns) {
            if (pattern.matcher(packName).matches()) {
                debugLog("Filtering out shader pack: " + packName);
                return false;
            }
        }
        return true;
    }

    public boolean matchesPattern(String packName, String patternStr) {
        return ShaderPatternUtil.matchesPattern(packName, patternStr);
    }
}
