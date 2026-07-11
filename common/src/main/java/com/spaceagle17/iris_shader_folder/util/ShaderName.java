package com.spaceagle17.iris_shader_folder.util;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.functionality.ShaderRenameSystem;
import com.spaceagle17.iris_shader_folder.functionality.ShaderRecolorSystem;

public class ShaderName {
    public static String renameShader(String originalPackName) {
        String recoloredName = ShaderRecolorSystem.getInstance().recolorShaderName(originalPackName);
        IrisShaderFolder.debugLog("[ShaderName] Recolored pack name: " + originalPackName + " -> " + recoloredName);
        String renamedName = ShaderRenameSystem.getInstance().renameShaderName(recoloredName);
        IrisShaderFolder.debugLog("[ShaderName] Renamed pack name: " + recoloredName + " -> " + renamedName);
        return renamedName;
    }
}
