package com.spaceagle17.iris_shader_folder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IrisShaderFolder implements ModInitializer {
    public static final String MOD_ID = "iris_shader_folder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = "1.1.0";

    public static Path shaderpacks = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
    private static IrisShaderFolder INSTANCE;
    public static boolean debugLoggingEnabled = false;
    public static List<String> filterPatterns = new ArrayList<>();
    public static List<String> reorderPatterns = new ArrayList<>();

    public static IrisShaderFolder getInstance() {
        return INSTANCE;
    }

    public void loadConfigOptions() {
        // Define all your config options here in one place
        debugLoggingEnabled = Boolean.parseBoolean(ConfigManager.readWriteConfig(
            "debugLogging",
            "false",
            "Enable debug logging (creates a debug file with detailed pattern processing)"
        ));

        // Handle the filter patterns section in a dynamic way
        if (ConfigManager.getSectionItems("filter").isEmpty()) {
            // If the section doesn't exist or is empty, create it with example content
            String filterDescription =
                "List of shader patterns to filter out, one per line\n" +
                "Examples:\n" +
                "  - Exact Match: Test (only \"Test\" will be filtered)\n" +
                "  - With version placeholder: ComplementaryReimagined_r{version}\n" +
                "  - With regex: Complementary{.*}\n" +
                "{version} matches any version number pattern like 1.2.3 or 4.5\n" +
                "Other {xyz} are treated as regex patterns (very powerful, be careful!)\n" +
                ".zip extensions are handled automatically\n";

            String defaultContent =
                "# Add filter patterns here, one per line\n" +
                "# test\n" +
                "# Complementary\n" +
                "# BSL{.*}";

            ConfigManager.writeSection("filter", defaultContent, filterDescription);
        }
        filterPatterns = ConfigManager.getSectionItems("filter");

        if (ConfigManager.getSectionItems("reorder").isEmpty()) {
            // If the section doesn't exist or is empty, create it with example content
            String reorderDescription =
                "List of shaderpacks to reorder in the shaderpacks selection menu, one per line\n" +
                "Format: <pattern> -> <position>\n" +
                "  - <pattern>: Shaderpack name or pattern (supports {version} and custom regex in braces)\n" +
                "  - <position>: 1-based index (1 = first slot, 2 = second, etc.)\n" +
                "Examples:\n" +
                "  - Exact match: Euphoria-Patches -> 1\n" +
                "  - With version: ComplementaryReimagined_r{version} -> 2\n" +
                "  - With regex: BSL{.*} -> 2\n" +
                "If multiple shaderpacks match a pattern, they are inserted at the given position, sorted alphabetically.\n" +
                "{version} matches any version number pattern like 1.2.3 or 4.5\n" +
                "Other {xyz} are treated as regex patterns (very powerful, be careful!)\n" +
                ".zip extensions are handled automatically\n";

            String defaultContent =
                "# Add reorder patterns here, one per line\n" +
                "# Euphoria-Patches -> 1\n" +
                "# Complementary{.*}_r{version} -> 2\n" +
                "# BSL{.*} -> 3";

            ConfigManager.writeSection("reorder", defaultContent, reorderDescription);
        }
        reorderPatterns = ConfigManager.getSectionItems("reorder");
    }

    public List<String> getFilterPatterns() {
        return filterPatterns;
    }

    public List<String> getReorderPatterns() {
        return reorderPatterns;
    }

    @Override
    public void onInitialize() {
        INSTANCE = this;

        // Initial configuration loading
        ConfigManager.loadProperties();
        loadConfigOptions();

        // Start the config watcher
        ConfigManager.startConfigWatcher();

        LOGGER.info("Hello from Iris Shader Folder Mod v" + VERSION);
    }

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
            boolean containsSpacEagle = shaderpacks.toString().contains("SpacEagle");
            Path euphoriaFolder = shaderpacks.resolve("Euphoria-Patches");
            boolean hasEuphoriaFolder = Files.exists(euphoriaFolder) && Files.isDirectory(euphoriaFolder);
            return containsSpacEagle && hasEuphoriaFolder;
        } catch (Exception ignored) {
            return false;
        }
    }
}