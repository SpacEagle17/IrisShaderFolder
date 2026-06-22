package com.spaceagle17.iris_shader_folder.forge;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ModLoaderSpecifics;
import net.minecraftforge.fml.common.Mod;

@Mod("iris_shader_folder")
public class ClientIrisShaderFolder {

    public ClientIrisShaderFolder() {
        ForgeModLoaderSpecifics forgeSpecifics = new ForgeModLoaderSpecifics();
        ModLoaderSpecifics.setInstance(forgeSpecifics);

        if (ModLoaderSpecifics.serverCheckStatic()) return;

        new IrisShaderFolder();
    }
}
