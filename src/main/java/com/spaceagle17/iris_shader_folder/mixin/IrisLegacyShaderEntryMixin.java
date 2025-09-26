package com.spaceagle17.iris_shader_folder.mixin;

import com.spaceagle17.iris_shader_folder.ShaderRecolorSystem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.coderbot.iris.gui.element.ShaderPackSelectionList$ShaderPackEntry", remap = false)
public class IrisLegacyShaderEntryMixin {
    @ModifyVariable(
        method = {
            "render",
            "renderContent",
            "method_25343",
            "m_6311_"
        },
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0,
        name = "name",
        remap = false
    )
    private String modifyNameVariable(String name) {
        return ShaderRecolorSystem.getInstance().recolorShaderName(name);
    }
}