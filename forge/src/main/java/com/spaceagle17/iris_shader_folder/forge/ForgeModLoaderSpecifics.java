package com.spaceagle17.iris_shader_folder.forge;

import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgeModLoaderSpecifics extends ModLoaderSpecifics {

    private final Path configDirectory;

    public ForgeModLoaderSpecifics() {
        this.configDirectory = FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getInstanceName() {
        return ModLoaderSpecifics.FORGE;
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
