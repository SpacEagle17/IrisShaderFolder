package com.spaceagle17.iris_shader_folder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ShaderTooltipSystem {
    private static ShaderTooltipSystem INSTANCE;
    
    private final Map<String, String> tooltipCache = new HashMap<>();
    private final Map<String, String> packJsonDescriptionCache = new HashMap<>();
    
    private List<TooltipRule> tooltipRules = new ArrayList<>();
    private List<String> lastTooltipPatterns = new ArrayList<>();
    
    private static final Gson GSON = new Gson();
    
    private long lastCacheRefreshTime = 0;
    private static final long CACHE_REFRESH_INTERVAL = 5000;
    private boolean rulesInitialized = false;
    
    private ShaderTooltipSystem() {
        updateRules();
    }
    
    public static ShaderTooltipSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderTooltipSystem();
        }
        return INSTANCE;
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
                // Split by [|] to separate shader pattern from tooltip text
                String[] parts = rule.split("\\s*\\[\\|\\]\\s*", 2);
                
                if (parts.length != 2) {
                    IrisShaderFolder.LOGGER.error("Invalid tooltip rule format: {}", rule);
                    continue;
                }
                
                String shaderPattern = parts[0].trim();
                String tooltipText = parts[1].trim();
                
                tooltipRules.add(new TooltipRule(shaderPattern, tooltipText));
                ShaderPatternUtil.logDebug("Added tooltip rule for pattern: " + shaderPattern);
            } catch (Exception e) {
                IrisShaderFolder.LOGGER.error("Error parsing tooltip rule: " + rule, e);
            }
        }
        
        rulesInitialized = true;
    }
    
    public String getTooltip(String shaderName) {
        ConfigManager.checkForUpdates();
        updateRules();
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheRefreshTime > CACHE_REFRESH_INTERVAL) {
            clearCache();
            lastCacheRefreshTime = currentTime;
        }
        
        if (tooltipCache.containsKey(shaderName)) {
            return tooltipCache.get(shaderName);
        }
        
        String packJsonDescription = getPackJsonDescription(shaderName);
        
        StringBuilder tooltipBuilder = new StringBuilder();
        
        if (packJsonDescription != null && !packJsonDescription.isEmpty()) {
            tooltipBuilder.append(packJsonDescription);
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
    
    private String getPackJsonDescription(String shaderName) {
        if (packJsonDescriptionCache.containsKey(shaderName)) {
            return packJsonDescriptionCache.get(shaderName);
        }
        
        String description = null;
        
        try {
            String baseName = shaderName;
            if (baseName.toLowerCase().endsWith(".zip")) {
                baseName = baseName.replace(".zip", "");
            }
            
            Path folderPath = IrisShaderFolder.shaderpacks.resolve(baseName);
            Path packJsonPath = folderPath.resolve("shaders/pack.json");
            
            if (Files.exists(packJsonPath)) {
                try {
                    String content = new String(Files.readAllBytes(packJsonPath));
                    description = extractDescription(content);
                } catch (IOException e) {
                    if (IrisShaderFolder.debugLoggingEnabled) {
                        IrisShaderFolder.LOGGER.error("Error reading pack.json from folder: " + folderPath, e);
                    }
                }
            } else {
                Path zipPath = IrisShaderFolder.shaderpacks.resolve(baseName + ".zip");
                if (Files.exists(zipPath)) {
                    try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                        ZipEntry packJsonEntry = zipFile.getEntry("shaders/pack.json");
                        if (packJsonEntry != null) {
                            try (InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(packJsonEntry))) {
                                JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                                if (jsonObject.has("shaderDescription")) {
                                    description = jsonObject.get("shaderDescription").getAsString();
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (IrisShaderFolder.debugLoggingEnabled) {
                            IrisShaderFolder.LOGGER.error("Error reading pack.json from zip: " + zipPath, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (IrisShaderFolder.debugLoggingEnabled) {
                IrisShaderFolder.LOGGER.error("Error getting pack.json description for: " + shaderName, e);
            }
        }
        
        packJsonDescriptionCache.put(shaderName, description);
        return description;
    }
    
    private String extractDescription(String jsonContent) {
        try {
            JsonObject jsonObject = GSON.fromJson(jsonContent, JsonObject.class);
            if (jsonObject.has("shaderDescription")) {
                return jsonObject.get("shaderDescription").getAsString();
            }
        } catch (JsonParseException e) {
            if (IrisShaderFolder.debugLoggingEnabled) {
                IrisShaderFolder.LOGGER.error("Error parsing pack.json", e);
            }
        }
        return null;
    }
    
    public void clearCache() {
        tooltipCache.clear();
        packJsonDescriptionCache.clear();
    }
    
    private static class TooltipRule {
        private final String shaderPattern;
        private final String tooltipText;
        
        public TooltipRule(String shaderPattern, String tooltipText) {
            this.shaderPattern = shaderPattern;
            this.tooltipText = tooltipText;
        }
        
        public String getShaderPattern() {
            return shaderPattern;
        }
        
        public String getTooltipText() {
            return tooltipText;
        }
    }
}