package com.spaceagle17.iris_shader_folder.fabric;

import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricModLoaderSpecifics extends ModLoaderSpecifics {

    private final Path configDirectory;

    public FabricModLoaderSpecifics() {
        this.configDirectory = FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getInstanceName() {
        return ModLoaderSpecifics.FABRIC;
    }

    @Override
    public Path getConfigDirectory() {
        return configDirectory;
    }

    @Override
    public boolean serverCheck() {
        try {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
        } catch (Throwable t) {
            return false;
        }
    }
}
