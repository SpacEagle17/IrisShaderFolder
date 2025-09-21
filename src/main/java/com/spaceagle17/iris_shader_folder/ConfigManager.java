package com.spaceagle17.iris_shader_folder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILENAME = "iris_shader_folder.properties";
    private static final String FILTER_START_MARKER = "filterStart:[";
    private static final String FILTER_END_MARKER = "]:filterEnd";
    
    private static ConfigManager INSTANCE;
    private final Properties properties = new Properties();
    private final File configFile;
    private long lastModified = 0;
    private List<String> filterPatterns = new ArrayList<>();
    private boolean debugLoggingEnabled = false;
    
    private ConfigManager() {
        File configDir = new File("config");
        if (!configDir.exists() && !configDir.mkdirs()) {
            IrisShaderFolder.LOGGER.error("Failed to create config directory");
        }
        
        this.configFile = new File(configDir, CONFIG_FILENAME);
        loadConfig();
    }
    
    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigManager();
        }
        return INSTANCE;
    }
    
    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
            return;
        }
        
        try {
            // Load general properties first
            properties.clear();
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);
            }
            
            // Process debug setting
            debugLoggingEnabled = Boolean.parseBoolean(properties.getProperty("debugLogging", "false"));
            
            // Then load filter patterns
            loadFilterPatterns();
            lastModified = configFile.lastModified();
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Failed to load config", e);
            createDefaultConfig();
        }
    }
    
    private void loadFilterPatterns() throws IOException {
        filterPatterns.clear();
        boolean inFilterSection = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.equals(FILTER_START_MARKER)) {
                    inFilterSection = true;
                } else if (line.equals(FILTER_END_MARKER)) {
                    inFilterSection = false;
                } else if (inFilterSection && !line.isEmpty() && !line.startsWith("#")) {
                    filterPatterns.add(line);
                }
            }
        }
        
        IrisShaderFolder.LOGGER.info("Loaded " + filterPatterns.size() + " filter patterns");
    }
    
    private void createDefaultConfig() {
        try {
            String defaultConfig = 
                "# Iris Shader Folder - Configuration File\n\n" +
                "# Enable debug logging (creates a debug file with detailed pattern processing)\n" +
                "debugLogging=false\n\n" +
                "# List of shader patterns to filter out, one per line\n" +
                "# Examples:\n" +
                "#   - Exact Match: Test (only \"Test\" will be filtered)\n" +
                "#   - With version placeholder: ComplementaryReimagined_r{version}\n" +
                "#   - With regex: Complementary{.*}\n" +
                "# {version} matches any version number pattern like 1.2.3 or 4.5\n" +
                "# Other {xyz} are treated as regex patterns (very powerful, be careful!)\n" +
                "# .zip extensions are handled automatically\n\n" +
                FILTER_START_MARKER + "\n" +
                "# Add filter patterns here, one per line\n" +
                "# test\n" +
                "# Complementary\n" +
                "# BSL{.*}\n" +
                FILTER_END_MARKER + "\n";
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig);
            }
            
            // Load the created config
            properties.clear();
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);
            }
            
            debugLoggingEnabled = Boolean.parseBoolean(properties.getProperty("debugLogging", "false"));
            loadFilterPatterns();
            lastModified = configFile.lastModified();
        } catch (IOException e) {
            IrisShaderFolder.LOGGER.error("Failed to create default config", e);
        }
    }
    
    /**
     * Checks if the config file has been modified since it was last loaded
     * and reloads it if necessary.
     * 
     * @return true if the config was reloaded
     */
    public boolean checkForUpdates() {
        if (configFile.exists() && configFile.lastModified() > lastModified) {
            if (debugLoggingEnabled) {
                IrisShaderFolder.LOGGER.info("Config file changed, reloading...");
            }
            loadConfig();
            return true;
        }
        return false;
    }
    
    public List<String> getFilterPatterns() {
        checkForUpdates(); // Check for file changes before returning patterns
        return filterPatterns;
    }
    
    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }
}