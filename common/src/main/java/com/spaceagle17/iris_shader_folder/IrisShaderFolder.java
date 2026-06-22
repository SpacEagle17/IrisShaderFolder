package com.spaceagle17.iris_shader_folder;

import com.spaceagle17.iris_shader_folder.config.ConfigManager;
import com.spaceagle17.iris_shader_folder.config.LoadConfig;
import com.spaceagle17.iris_shader_folder.logging.IrisShaderFolderLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class IrisShaderFolder {
    public static final String MOD_ID = "iris_shader_folder";
    public static final String VERSION = "1.3.2";

    public static Path shaderpacks;
    private static IrisShaderFolder INSTANCE;
    private static IrisShaderFolderLogger loggerInstance;

    public IrisShaderFolder() {
        INSTANCE = this;
        shaderpacks = ModLoaderSpecifics.configDirectory().getParent().resolve("shaderpacks");
        loggerInstance = new IrisShaderFolderLogger();
        log(0, "IrisShaderFolder v" + VERSION + " initialized.");
        ConfigManager.loadProperties();
        LoadConfig.loadConfigOptions();
        ConfigManager.startConfigWatcher();
    }

    public static IrisShaderFolder getInstance() {
        return INSTANCE;
    }

    public static void log(int messageLevel, String message) {
        if (loggerInstance == null) {
            System.out.println("IrisShaderFolder (early log): " + message);
            return;
        }
        loggerInstance.log(messageLevel, message);
    }

    public static void debugLog(String message) {
        if (LoadConfig.debugLoggingEnabled) {
            log(0, message);
        }
    }

    public List<String> getFilterPatterns() { return LoadConfig.filterPatterns; }
    public List<String> getReorderPatterns() { return LoadConfig.reorderPatterns; }
    public List<String> getRenamePatterns() { return LoadConfig.renamePatterns; }
    public List<String> getRecolorPatterns() { return LoadConfig.recolorPatterns; }
    public List<String> getTooltipPatterns() { return LoadConfig.tooltipPatterns; }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ConfigManager.stopConfigWatcher();
            } catch (Exception ignored) {
            }
        }));
    }

    public static boolean isSpacEagle() {
        try {
            boolean containsSpacEagle = shaderpacks.toString().toLowerCase(Locale.ROOT).contains("spaceagle");
            Path euphoriaFolder = shaderpacks.resolve("Euphoria-Patches");
            boolean hasEuphoriaFolder = Files.exists(euphoriaFolder) && Files.isDirectory(euphoriaFolder);
            return containsSpacEagle && hasEuphoriaFolder;
        } catch (Exception ignored) {
            return false;
        }
    }
}
