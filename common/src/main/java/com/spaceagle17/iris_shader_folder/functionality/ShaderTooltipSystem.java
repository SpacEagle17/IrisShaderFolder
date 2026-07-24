package com.spaceagle17.iris_shader_folder.functionality;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.config.ConfigManager;
import com.spaceagle17.iris_shader_folder.util.IrisLanguageAccess;
import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ShaderTooltipSystem implements ConfigManager.ConfigUpdateListener {
    private static ShaderTooltipSystem INSTANCE;

    private static final String SHADER_DESCRIPTION_KEY = "shaderDescription";

    private final Map<String, String> tooltipCache = new HashMap<>();
    private final Map<String, String> descriptionCache = new HashMap<>();

    private List<TooltipRule> tooltipRules = new ArrayList<>();
    private List<String> lastTooltipPatterns = new ArrayList<>();

    private static final Gson GSON = new Gson();

    private long lastCacheRefreshTime = 0;
    private static final long CACHE_REFRESH_INTERVAL = 5000;
    private boolean rulesInitialized = false;

    private ShaderTooltipSystem() {
        ConfigManager.registerUpdateListener(this);
        updateRules();
    }

    public static ShaderTooltipSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderTooltipSystem();
        }
        return INSTANCE;
    }

    private static void debugLog(String message) {
        IrisShaderFolder.debugLog("[ShaderTooltipSystem]" + message);
    }

    public void updateRules() {
        List<String> tooltipPatterns = IrisShaderFolder.getInstance().getTooltipPatterns();
        boolean configChanged = !tooltipPatterns.equals(lastTooltipPatterns);

        if (!configChanged && rulesInitialized) {
            return;
        }

        lastTooltipPatterns = new ArrayList<>(tooltipPatterns);
        tooltipRules.clear();
        clearCache();

        for (String rule : tooltipPatterns) {
            rule = rule.trim();
            if (rule.isEmpty() || rule.startsWith("#")) continue;

            try {
                String[] parts = rule.split("\\s*\\[\\|\\]\\s*", 2);

                if (parts.length != 2) {
                    IrisShaderFolder.log(3, "Invalid tooltip rule format: " + rule);
                    continue;
                }

                String shaderPattern = parts[0].trim();
                String tooltipText = parts[1].trim();

                tooltipRules.add(new TooltipRule(shaderPattern, tooltipText));
                debugLog("Added tooltip rule for pattern: " + shaderPattern);
            } catch (Exception e) {
                IrisShaderFolder.log(3, "Error parsing tooltip rule: " + rule + ": " + e.getMessage());
            }
        }

        rulesInitialized = true;
    }

    public String getTooltip(String shaderName) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheRefreshTime > CACHE_REFRESH_INTERVAL) {
            clearCache();
            lastCacheRefreshTime = currentTime;
        }

        if (tooltipCache.containsKey(shaderName)) {
            return tooltipCache.get(shaderName);
        }

        String description = getDescription(shaderName);

        StringBuilder tooltipBuilder = new StringBuilder();

        if (description != null && !description.isEmpty()) {
            tooltipBuilder.append(description);
        }

        for (TooltipRule rule : tooltipRules) {
            if (ShaderPatternUtil.matchesPattern(shaderName, rule.getShaderPattern())) {
                if (tooltipBuilder.length() > 0) {
                    tooltipBuilder.append("\n");
                }
                tooltipBuilder.append(rule.getTooltipText());
            }
        }

        String tooltip = tooltipBuilder.toString();
        tooltipCache.put(shaderName, tooltip);
        return tooltip;
    }

    /**
     * Resolves the shader pack's description, searching Iris lang files first
     * (active locale down to en_us) and {@code shaders/pack.json} as a fallback.
     * Opens zipped packs at most once per call.
     */
    private String getDescription(String shaderName) {
        if (descriptionCache.containsKey(shaderName)) {
            return descriptionCache.get(shaderName);
        }

        String description = null;

        try {
            String baseName = shaderName;
            if (baseName.toLowerCase().endsWith(".zip")) {
                baseName = baseName.replace(".zip", "");
            }

            List<String> codes = new ArrayList<>(IrisLanguageAccess.getActiveLanguageCodes());
            if (!codes.contains("en_us")) {
                codes.add("en_us");
            }

            Path folderPath = IrisShaderFolder.shaderpacks.resolve(baseName);
            if (Files.isDirectory(folderPath)) {
                description = getLangFileDescriptionFromFolder(folderPath, codes);
                if (description == null || description.isEmpty()) {
                    debugLog("Shader description not found in lang files for shader: " + baseName);
                    description = getPackJsonDescriptionFromFolder(folderPath);
                }
            } else {
                Path zipPath = IrisShaderFolder.shaderpacks.resolve(baseName + ".zip");
                if (Files.exists(zipPath)) {
                    try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                        description = getLangFileDescriptionFromZip(zipFile, codes);
                        if (description == null || description.isEmpty()) {
                            debugLog("Shader description not found in lang files for shader: " + baseName);
                            description = getPackJsonDescriptionFromZip(zipFile, zipPath);
                        }
                    } catch (Exception e) {
                        debugLog("Error reading shader pack zip: " + zipPath + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            debugLog("Error getting description for: " + shaderName + ": " + e.getMessage());
        }

        descriptionCache.put(shaderName, description);
        return description;
    }

    private String getLangFileDescriptionFromFolder(Path folderPath, List<String> codes) {
        for (String code : codes) {
            Path langPath = folderPath.resolve("shaders/lang/" + code + ".lang");
            if (!Files.exists(langPath)) continue;

            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(langPath), StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(reader);
                String value = properties.getProperty(SHADER_DESCRIPTION_KEY);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            } catch (IOException e) {
                debugLog("Error reading lang file: " + langPath + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String getLangFileDescriptionFromZip(ZipFile zipFile, List<String> codes) {
        for (String code : codes) {
            ZipEntry langEntry = zipFile.getEntry("shaders/lang/" + code + ".lang");
            if (langEntry == null) continue;

            try (InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(langEntry), StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(reader);
                String value = properties.getProperty(SHADER_DESCRIPTION_KEY);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            } catch (IOException e) {
                debugLog("Error reading lang entry from zip: " + e.getMessage());
            }
        }
        return null;
    }

    private String getPackJsonDescriptionFromFolder(Path folderPath) {
        Path packJsonPath = folderPath.resolve("shaders/pack.json");
        if (!Files.exists(packJsonPath)) return null;

        try {
            String content = new String(Files.readAllBytes(packJsonPath));
            return extractDescription(content);
        } catch (IOException e) {
            debugLog("Error reading pack.json from folder: " + folderPath + ": " + e.getMessage());
            return null;
        }
    }

    private String getPackJsonDescriptionFromZip(ZipFile zipFile, Path zipPath) {
        ZipEntry packJsonEntry = zipFile.getEntry("shaders/pack.json");
        if (packJsonEntry == null) return null;

        try (InputStream packJsonStream = zipFile.getInputStream(packJsonEntry)) {
            String content = readUtf8(packJsonStream);
            return extractDescription(content);
        } catch (IOException e) {
            debugLog("Error reading pack.json from zip: " + zipPath + ": " + e.getMessage());
            return null;
        }
    }

    private String readUtf8(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[2048];

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, charsRead);
            }
        }

        return content.toString();
    }

    private String extractDescription(String jsonContent) {
        try {
            JsonObject jsonObject = GSON.fromJson(jsonContent, JsonObject.class);
            if (jsonObject.has("shaderDescription")) {
                return jsonObject.get("shaderDescription").getAsString();
            }
            if (jsonObject.has("description")) {
                return jsonObject.get("description").getAsString();
            }
        } catch (JsonParseException e) {
            debugLog("Error parsing pack.json: " + e.getMessage());
        }
        return null;
    }

    public void clearCache() {
        tooltipCache.clear();
        descriptionCache.clear();
    }

    private static class TooltipRule {
        private final String shaderPattern;
        private final String tooltipText;

        public TooltipRule(String shaderPattern, String tooltipText) {
            this.shaderPattern = shaderPattern;
            this.tooltipText = tooltipText;
        }

        public String getShaderPattern() { return shaderPattern; }
        public String getTooltipText() { return tooltipText; }
    }

    @Override
    public void onConfigUpdate() {
        updateRules();
    }
}
