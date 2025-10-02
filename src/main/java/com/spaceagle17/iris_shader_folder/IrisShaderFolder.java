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
    public static final String VERSION = "1.2.1";

    public static Path shaderpacks = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
    private static IrisShaderFolder INSTANCE;
    public static boolean debugLoggingEnabled = false;
    public static List<String> filterPatterns = new ArrayList<>();
    public static List<String> reorderPatterns = new ArrayList<>();
    public static List<String> recolorPatterns = new ArrayList<>();
    public static List<String> tooltipPatterns = new ArrayList<>();

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
                "The position is determined by line order (first line = position 1, etc.)\n" +
                "Examples:\n" +
                "  - First position: {.*}EuphoriaPatches{.*}\n" +
                "  - Second position: Complementary{.*}_r{version}\n" +
                "  - Third position: BSL{.*}\n" +
                "If multiple shaderpacks match a pattern, they are inserted at the given position, sorted alphabetically.\n" +
                "{version} matches any version number pattern like 1.2.3 or 4.5\n" +
                "Other {xyz} are treated as regex patterns (very powerful, be careful!)\n" +
                ".zip extensions are handled automatically\n";

            String defaultContent =
                "# Add reorder patterns here, one per line\n" +
                "# {.*}EuphoriaPatches{.*}\n" +
                "# Complementary{.*}_r{version}\n" +
                "# BSL{.*}";

            ConfigManager.writeSection("reorder", defaultContent, reorderDescription);
        }
        reorderPatterns = ConfigManager.getSectionItems("reorder");

        if (ConfigManager.getSectionItems("recolor").isEmpty()) {
            // If the section doesn't exist or is empty, create it with example content
            String recolorDescription = 
                "List of recoloring rules for shaderpack names in the selection menu\n" +
                "Each rule recolors either a specific part of the shaderpack name or the entire name.\n" +
                "{version} matches any version number pattern like 1.2.3 or 4.5\n" +
                "Other {xyz} are treated as regex patterns (very powerful, be careful!)\n" +
                "Format: shader_pattern [|] part_pattern [->] color_name [|] part_pattern2 [->] color_name2 ....\n" +
                "  - shader_pattern: Matches shaderpack names (exact or with {regex})\n" +
                "  - part_pattern: Matches the part of the name to recolor (exact or with {regex})\n" +
                "      - Use {all} to recolor the entire name\n" +
                "  - color_name: One of the official Minecraft color names or Minecraft color codes:\n" +
                "    black (§0), dark_blue (§1), dark_green (§2), dark_aqua (§3), dark_red (4), dark_purple (§5), gold (§6), gray (§7),\n" +
                "    dark_gray (§8), blue (§9), green (§a), aqua (§b), red (§c), light_purple (§d), yellow (§e), white (§f)\n" +
                "  - The \"part_pattern [->] color_name\" combination can be repeated as often as desired to get multiple colors in the same name\n" +
                "\n" +
                "Examples:\n" +
                "  - Complementary{.*} [|] Comp [->] red [|] {version} [->] §6\n" +
                "      Recolors the \"Comp\" part to red and the version part in any Complementary shaderpack name to gold.\n" +
                "  - {.*}EuphoriaPatches{.*} [|] EuphoriaPatches_{version} [->] light_purple\n" +
                "      Recolors the \"EuphoriaPatches_{version}\" part in any shader with EuphoriaPatches in the name to light_purple.\n" +
                "  - test [|] {all} [->] red\n" +
                "      Recolors the entire name \"test\" to red.\n";

            String defaultContent =
                "# Add recolor rules here, one per line\n" +
                "# Complementary{.*} [|] Comp [->] red [|] {version} [->] §6\n" +
                "# {.*}EuphoriaPatches{.*} [|] EuphoriaPatches_{version} [->] light_purple\n" +
                "# test [|] {all} [->] red";

            ConfigManager.writeSection("recolor", defaultContent, recolorDescription);
        }
        recolorPatterns = ConfigManager.getSectionItems("recolor");
        
        // Add new tooltip section
        if (ConfigManager.getSectionItems("tooltip").isEmpty()) {
            // If the section doesn't exist or is empty, create it with example content
            String tooltipDescription = 
                "List of tooltip rules for shaderpacks in the selection menu\n" +
                "Format: shader_pattern [|] tooltip_text\n" +
                "  - shader_pattern: Matches shaderpack names (exact or with {regex})\n" +
                "  - tooltip_text: Text to display when hovering over the shader\n" +
                "\n" +
                "Note: If a shader has its own description in its pack.json file, that description will be\n" +
                "shown first, followed by your custom tooltip text if configured here.\n" +
                "\n" +
                "Examples:\n" +
                "  - Complementary{.*} [|] Complementary is a shaderpack focused on performance and visual quality.\n" +
                "  - {.*}EuphoriaPatches{.*} [|] A powerful add-on for Complementary Shaders.\n" +
                "  - test [|] This is a test shaderpack.";

            String defaultContent =
                "# Add tooltip rules here, one per line\n" +
                "# Complementary{.*} [|] Complementary is a shaderpack focused on performance and visual quality.\n" +
                "# test [|] This is a test shaderpack.";

            ConfigManager.writeSection("tooltip", defaultContent, tooltipDescription);
        }
        tooltipPatterns = ConfigManager.getSectionItems("tooltip");
    }

    public List<String> getFilterPatterns() {
        return filterPatterns;
    }

    public List<String> getReorderPatterns() {
        return reorderPatterns;
    }

    public List<String> getRecolorPatterns() {
        return recolorPatterns;
    }
    
    public List<String> getTooltipPatterns() {
        return tooltipPatterns;
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