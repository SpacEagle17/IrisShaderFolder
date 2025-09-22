package com.spaceagle17.iris_shader_folder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    private static final String CONFIG_FILENAME = "iris_shader_folder.properties";
    private static final Path CONFIG_PATH = Paths.get("config", CONFIG_FILENAME);
    private static final Properties properties = new Properties();
    private static FileTime lastModified = null;
    private static boolean watcherActive = false;
    private static ScheduledExecutorService scheduler;

    private static void debugLog(String message) {
        if (IrisShaderFolder.debugLoggingEnabled) {
            IrisShaderFolder.LOGGER.info("[Config] " + message);
        }
    }

    public static void createConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.createFile(CONFIG_PATH);
            writeInitialConfig();
            IrisShaderFolder.LOGGER.info("Successfully created config file");
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Error creating config file: " + e.getMessage());
        }
    }

    private static void writeInitialConfig() throws IOException {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toString(), false)) {
            writer.write("# Iris Shader Folder - Configuration File\n");
            writer.write("# Made for version " + IrisShaderFolder.VERSION + "\n");
            writer.write("# Thank you for using Iris Shader Folder\n");
        }
    }

    public static void updateVersionLine() {
        try {
            List<String> lines = Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8);
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

            Files.write(CONFIG_PATH, lines, StandardCharsets.UTF_8);
            debugLog("Successfully updated version info in config file");
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Error updating config file with version: " + e.getMessage());
        }
    }

    public static void writeConfig(String option, String value, String description) {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createConfig();
            } else {
                updateVersionLine();
            }
            
            loadProperties();
            
            if (!properties.containsKey(option)) {
                List<String> lines = Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8);
                try (FileWriter writer = new FileWriter(CONFIG_PATH.toString(), false)) {
                    // Write existing lines
                    for (String line : lines) {
                        writer.write(line + "\n");
                    }

                    // Add new configuration
                    writer.write("\n"); // Add newline before new entry
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
            IrisShaderFolder.LOGGER.error("Error writing to config file: " + e.getMessage());
        }
    }

    public static String readWriteConfig(String optionName, String defaultValue, String description) {
        writeConfig(optionName, defaultValue, description);
        return properties.getProperty(optionName, defaultValue);
    }

    public static void loadProperties() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createConfig();
                return;
            }
            
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                properties.clear();
                properties.load(in);
                lastModified = Files.getLastModifiedTime(CONFIG_PATH);
            }
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Error loading properties: " + e.getMessage());
        }
    }

    /**
     * Writes a section to the config file
     */
    public static void writeSection(String sectionName, String content, String description) {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createConfig();
            }
            
            List<String> lines = Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8);
            String startMarker = sectionName + "Start:[";
            String endMarker = "]:" + sectionName + "End";
            
            int startIndex = -1;
            int endIndex = -1;
            
            // Find existing section
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals(startMarker)) {
                    startIndex = i;
                } else if (lines.get(i).trim().equals(endMarker) && startIndex != -1) {
                    endIndex = i;
                    break;
                }
            }
            
            // Check if content is already the same (to avoid unnecessary writes)
            if (startIndex != -1 && endIndex != -1) {
                StringBuilder existingContent = new StringBuilder();
                for (int i = startIndex + 1; i < endIndex; i++) {
                    if (existingContent.length() > 0) {
                        existingContent.append("\n");
                    }
                    existingContent.append(lines.get(i));
                }
                
                // If content is the same, don't rewrite
                if (existingContent.toString().equals(String.join("\n", content.split("\n")))) {
                    return;
                }
            }
            
            // Create a new list for the updated content
            List<String> newLines = new ArrayList<>();
            
            if (startIndex != -1 && endIndex != -1) {
                // Section exists, update it
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
                // Section doesn't exist, add it at the end
                newLines.addAll(lines);
                
                if (newLines.size() > 0 && !newLines.get(newLines.size() - 1).trim().isEmpty()) {
                    newLines.add(""); // Add empty line before new section
                }
                
                // Add description
                newLines.add("#--------------------------------------------------------------------------------");
                if (description != null) {
                    for (String line : description.split("\n")) {
                        newLines.add("# " + line);
                    }
                    newLines.add("");
                }
                
                // Add the section
                newLines.add(startMarker);
                for (String line : content.split("\n")) {
                    newLines.add(line);
                }
                newLines.add(endMarker);
            }
            
            // Write the updated file
            Files.write(CONFIG_PATH, newLines, StandardCharsets.UTF_8);
            lastModified = Files.getLastModifiedTime(CONFIG_PATH);
            
            debugLog("Successfully wrote section: " + sectionName);
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Error writing section to config: " + e.getMessage());
        }
    }
    
    /**
     * Reads a section from the config file
     */
    public static String readSection(String sectionName) {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                return "";
            }
            
            List<String> lines = Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8);
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
                    if (content.length() > 0) {
                        content.append("\n");
                    }
                    content.append(line);
                }
            }
            
            return content.toString();
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Error reading section from config: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Gets all lines from a section that are not comments or empty
     */
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
    
    public static void startConfigWatcher() {
        if (watcherActive) return;

        watcherActive = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "IrisShaderFolderConfigWatcher");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (Files.exists(CONFIG_PATH)) {
                    FileTime currentModified = Files.getLastModifiedTime(CONFIG_PATH);
                    if (lastModified == null || !currentModified.equals(lastModified)) {
                        debugLog("Config file changed, reloading settings");
                        loadProperties();
                        
                        IrisShaderFolder instance = IrisShaderFolder.getInstance();
                        if (instance != null) {
                            instance.loadConfigOptions();
                        }
                    }
                }
            } catch (IOException ignored) {}
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    public static void stopConfigWatcher() {
        if (watcherActive && scheduler != null) {
            scheduler.shutdown();
            watcherActive = false;
        }
    }

    public static boolean checkForUpdates() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                FileTime currentModified = Files.getLastModifiedTime(CONFIG_PATH);
                if (lastModified == null || !currentModified.equals(lastModified)) {
                    loadProperties();
                    
                    IrisShaderFolder instance = IrisShaderFolder.getInstance();
                    if (instance != null) {
                        instance.loadConfigOptions();
                    }
                    
                    return true;
                }
            }
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Error checking for config updates: " + e.getMessage());
        }
        return false;
    }
}