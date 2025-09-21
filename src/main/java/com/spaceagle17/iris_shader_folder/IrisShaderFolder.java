package com.spaceagle17.iris_shader_folder;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("iris_shader_folder")
public class IrisShaderFolder {
    public static final String MOD_ID = "IrisShaderFolder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public IrisShaderFolder() {
        ConfigManager.getInstance();
        ShaderFilterSystem.getInstance();
        LOGGER.info("Iris Shader Folder Mod initialized");
    }
}