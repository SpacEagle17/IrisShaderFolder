package com.spaceagle17.iris_shader_folder.fabric.util;

import com.spaceagle17.iris_shader_folder.fabric.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.fabric.ShaderRenameSystem;
import com.spaceagle17.iris_shader_folder.fabric.ShaderRecolorSystem;

public class ShaderName {
    public static String renameShader(String originalPackName) {
        String recoloredName = ShaderRecolorSystem.getInstance().recolorShaderName(originalPackName);
        if (IrisShaderFolder.debugLoggingEnabled) {
            System.out.println("[IrisShaderFolder] Recolored pack name: " + originalPackName + " -> " + recoloredName);
        }
        String renamedName = ShaderRenameSystem.getInstance().renameShaderName(recoloredName);
        if (IrisShaderFolder.debugLoggingEnabled) {
            System.out.println("[IrisShaderFolder] Renamed pack name: " + recoloredName + " -> " + renamedName);
        }
        return renamedName;
    }
}
