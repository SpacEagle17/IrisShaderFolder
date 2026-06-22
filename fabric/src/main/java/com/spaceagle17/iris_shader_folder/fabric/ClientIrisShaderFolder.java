package com.spaceagle17.iris_shader_folder.fabric;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;
import net.fabricmc.api.ModInitializer;

public class ClientIrisShaderFolder implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricModLoaderSpecifics fabricSpecifics = new FabricModLoaderSpecifics();
        ModLoaderSpecifics.setInstance(fabricSpecifics);

        if (ModLoaderSpecifics.serverCheckStatic()) return;

        new IrisShaderFolder();
    }
}
