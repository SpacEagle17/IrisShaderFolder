package com.spaceagle17.iris_shader_folder.config;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    private static final String CONFIG_FILENAME = "iris_shader_folder.properties";
    private static Path configPath;
    private static final Properties properties = new Properties();
    private static FileTime lastModified = null;
    private static boolean watcherActive = false;
    private static ScheduledExecutorService scheduler;

    private static final List<ConfigUpdateListener> updateListeners = new ArrayList<>();

    private static Path getConfigPath() {
        if (configPath == null) {
            configPath = ModLoaderSpecifics.configDirectory().resolve(CONFIG_FILENAME);
        }
        return configPath;
    }

    private static void debugLog(String message) {
        IrisShaderFolder.debugLog("[Config] " + message);
    }

    public interface ConfigUpdateListener {
        void onConfigUpdate();
    }

    public static void registerUpdateListener(ConfigUpdateListener listener) {
        if (!updateListeners.contains(listener)) {
            updateListeners.add(listener);
            debugLog("Registered config update listener: " + listener.getClass().getSimpleName());
        }
    }

    private static void notifyConfigUpdated() {
        debugLog("Notifying " + updateListeners.size() + " listeners of config update");
        for (ConfigUpdateListener listener : updateListeners) {
            try {
                listener.onConfigUpdate();
            } catch (Exception e) {
                IrisShaderFolder.log(3, "Error notifying listener " +
                    listener.getClass().getSimpleName() + " of config update: " + e.getMessage());
            }
        }
    }

    public static void createConfig() {
        try {
            Files.createDirectories(getConfigPath().getParent());
            Files.createFile(getConfigPath());
            writeInitialConfig();
            IrisShaderFolder.log(1, "Successfully created config file");
        } catch (IOException e) {
            IrisShaderFolder.log(3, "Error creating config file: " + e.getMessage());
        }
    }

    private static void writeInitialConfig() throws IOException {
        try (FileWriter writer = new FileWriter(getConfigPath().toString(), false)) {
            writer.write("# Iris Shader Folder - Configuration File\n");
            writer.write("# Made for version " + IrisShaderFolder.VERSION + "\n");
            writer.write("# Thank you for using Iris Shader Folder - SpacEagle17\n");
        }
    }

    public static void updateVersionLine() {
        try {
            List<String> lines = Files.readAllLines(getConfigPath(), StandardCharsets.UTF_8);
            boolean versionLineFound = false;

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("# Made for version")) {
                    if (lines.get(i).contains(IrisShaderFolder.VERSION)) return;
                    lines.set(i, "# Made for version " + IrisShaderFolder.VERSION);
                    versionLineFound = true;
                    break;
                }
            }

            if (!versionLineFound) {
                int headerIndex = lines.indexOf("# Iris Shader Folder - Configuration File");
                if (headerIndex >= 0) {
                    lines.add(headerIndex + 1, "# Made for version " + IrisShaderFolder.VERSION);
                }
            }

            Files.write(getConfigPath(), lines, StandardCharsets.UTF_8);
            debugLog("Successfully updated version info in config file");
        } catch (IOException e) {
            IrisShaderFolder.log(3, "Error updating config file with version: " + e.getMessage());
        }
    }

    public static void writeConfig(String option, String value, String description) {
        try {
            if (!Files.exists(getConfigPath())) {
                createConfig();
            } else {
                updateVersionLine();
            }

            loadProperties();

            if (!properties.containsKey(option)) {
                List<String> lines = Files.readAllLines(getConfigPath(), StandardCharsets.UTF_8);
                try (FileWriter writer = new FileWriter(getConfigPath().toString(), false)) {
                    for (String line : lines) {
                        writer.write(line + "\n");
                    }

                    writer.write("\n");
                    if (description != null) {
                        String[] descLines = description.split("\n");
                        for (String line : descLines) {
                            writer.write("# " + line + "\n");
                        }
                    }
                    writer.write(option + "=" + value + "\n");
                    debugLog("Successfully wrote to config file: " + option + "=" + value);
                }
            }
        } catch (IOException e) {
            IrisShaderFolder.log(3, "Error writing to config file: " + e.getMessage());
        }
    }

    public static String readWriteConfig(String optionName, String defaultValue, String description) {
        writeConfig(optionName, defaultValue, description);
        return properties.getProperty(optionName, defaultValue);
    }

    public static void loadProperties() {
        try {
            if (!Files.exists(getConfigPath())) {
                createConfig();
                return;
            }

            try (InputStream in = Files.newInputStream(getConfigPath())) {
                properties.clear();
                properties.load(in);
                lastModified = Files.getLastModifiedTime(getConfigPath());
            }
        } catch (IOException e) {
            IrisShaderFolder.log(3, "Error loading properties: " + e.getMessage());
        }
    }

    public static void writeSection(String sectionName, String content, String description) {
        try {
            if (!Files.exists(getConfigPath())) {
                createConfig();
            }

            List<String> lines = Files.readAllLines(getConfigPath(), StandardCharsets.UTF_8);
            String startMarker = sectionName + "Start:[";
            String endMarker = "]:" + sectionName + "End";

            int startIndex = -1;
            int endIndex = -1;

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals(startMarker)) {
                    startIndex = i;
                } else if (lines.get(i).trim().equals(endMarker) && startIndex != -1) {
                    endIndex = i;
                    break;
                }
            }

            if (startIndex != -1 && endIndex != -1) {
                StringBuilder existingContent = new StringBuilder();
                for (int i = startIndex + 1; i < endIndex; i++) {
                    if (existingContent.length() > 0) existingContent.append("\n");
                    existingContent.append(lines.get(i));
                }
                if (existingContent.toString().equals(String.join("\n", content.split("\n")))) {
                    return;
                }
            }

            List<String> newLines = new ArrayList<>();

            if (startIndex != -1 && endIndex != -1) {
                newLines.addAll(lines.subList(0, startIndex));
                newLines.add(startMarker);
                for (String line : content.split("\n")) {
                    newLines.add(line);
                }
                newLines.add(endMarker);
                if (endIndex + 1 < lines.size()) {
                    newLines.addAll(lines.subList(endIndex + 1, lines.size()));
                }
            } else {
                newLines.addAll(lines);

                if (newLines.size() > 0 && !newLines.get(newLines.size() - 1).trim().isEmpty()) {
                    newLines.add("");
                }

                newLines.add("#--------------------------------------------------------------------------------");
                if (description != null) {
                    for (String line : description.split("\n")) {
                        newLines.add("# " + line);
                    }
                    newLines.add("");
                }

                newLines.add(startMarker);
                for (String line : content.split("\n")) {
                    newLines.add(line);
                }
                newLines.add(endMarker);
            }

            Files.write(getConfigPath(), newLines, StandardCharsets.UTF_8);
            lastModified = Files.getLastModifiedTime(getConfigPath());

            debugLog("Successfully wrote section: " + sectionName);
        } catch (IOException e) {
            IrisShaderFolder.log(3, "Error writing section to config: " + e.getMessage());
        }
    }

    public static String readSection(String sectionName) {
        try {
            if (!Files.exists(getConfigPath())) {
                return "";
            }

            List<String> lines = Files.readAllLines(getConfigPath(), StandardCharsets.UTF_8);
            String startMarker = sectionName + "Start:[";
            String endMarker = "]:" + sectionName + "End";

            boolean inSection = false;
            StringBuilder content = new StringBuilder();

            for (String line : lines) {
                line = line.trim();
                if (line.equals(startMarker)) {
                    inSection = true;
                } else if (line.equals(endMarker)) {
                    inSection = false;
                } else if (inSection) {
                    if (content.length() > 0) content.append("\n");
                    content.append(line);
                }
            }

            return content.toString();
        } catch (IOException e) {
            IrisShaderFolder.log(3, "Error reading section from config: " + e.getMessage());
            return "";
        }
    }

    public static List<String> getSectionItems(String sectionName) {
        String sectionContent = readSection(sectionName);
        List<String> items = new ArrayList<>();

        for (String line : sectionContent.split("\n")) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                items.add(line);
            }
        }

        return items;
    }

    private static void processConfigUpdate() {
        try {
            if (Files.exists(getConfigPath())) {
                FileTime currentModified = Files.getLastModifiedTime(getConfigPath());
                if (!currentModified.equals(lastModified)) {
                    debugLog("Config file changed, reloading settings");
                    loadProperties();
                    LoadConfig.loadConfigOptions();

                    notifyConfigUpdated();
                }
            }
        } catch (IOException e) {
            IrisShaderFolder.log(3, "Error checking for config updates: " + e.getMessage());
        }
    }

    public static void startConfigWatcher() {
        if (watcherActive) return;

        watcherActive = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "IrisShaderFolderConfigWatcher");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(ConfigManager::processConfigUpdate, 1, 1, TimeUnit.SECONDS);
    }

    public static void stopConfigWatcher() {
        if (watcherActive && scheduler != null) {
            scheduler.shutdown();
            watcherActive = false;
        }
    }
}
