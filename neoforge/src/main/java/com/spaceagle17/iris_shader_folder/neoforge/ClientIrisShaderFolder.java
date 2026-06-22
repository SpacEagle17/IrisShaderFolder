package com.spaceagle17.iris_shader_folder.neoforge;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;
import net.neoforged.fml.common.Mod;

@Mod("iris_shader_folder")
public class ClientIrisShaderFolder {

    public ClientIrisShaderFolder() {
        NeoForgeModLoaderSpecifics neoforgeSpecifics = new NeoForgeModLoaderSpecifics();
        ModLoaderSpecifics.setInstance(neoforgeSpecifics);

        if (ModLoaderSpecifics.serverCheckStatic()) return;

        new IrisShaderFolder();
    }
}
