package com.spaceagle17.iris_shader_folder.neoforge;

import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;

import net.neoforged.api.distmarker.Dist;
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
        try {
            // Try to use getDist() if available (NeoForge 1.21.10+)
            java.lang.reflect.Method getDistMethod = FMLEnvironment.class.getMethod("getDist");
            Object dist = getDistMethod.invoke(null);
            if (dist == Dist.DEDICATED_SERVER) {
                System.err.println("[EuphoriaPatcher] Server Detected! The Euphoria Patcher Mod disables itself gracefully on a server. Disabling...");
                return true;
            }
        } catch (NoSuchMethodException e) {
            // Fallback for older NeoForge versions
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                System.err.println("[EuphoriaPatcher] Server Detected! The Euphoria Patcher Mod disables itself gracefully on a server. Disabling...");
                return true;
            }
        } catch (Throwable t) {
            // Any other error, assume not a server
        }
        return false;
    }
}
