package com.spaceagle17.iris_shader_folder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IrisShaderFolder implements ModInitializer {
    public static final String MOD_ID = "iris-shader-folder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigManager.getInstance();
        ShaderFilterSystem.getInstance();
        LOGGER.info("Iris Shader Folder Mod initialized");
    }
}