package com.spaceagle17.iris_shader_folder.neoforge;

import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgeModLoaderSpecifics extends ModLoaderSpecifics {

    private final Path configDirectory;

    public NeoForgeModLoaderSpecifics() {
        this.configDirectory = FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getInstanceName() {
        return ModLoaderSpecifics.NEOFORGE;
    }

    @Override
    public Path getConfigDirectory() {
        return configDirectory;
    }

    @Override
    public boolean serverCheck() {
        return FMLEnvironment.dist.isDedicatedServer();
    }
}
