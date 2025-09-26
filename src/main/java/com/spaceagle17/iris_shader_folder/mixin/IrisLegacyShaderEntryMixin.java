package com.spaceagle17.iris_shader_folder.mixin;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.coderbot.iris.gui.element.ShaderPackSelectionList$ShaderPackEntry", remap = false)
public class IrisLegacyShaderEntryMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    private String packName;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onConstructed(CallbackInfo ci) {
        System.out.println("Legacy ShaderPackEntry constructor called - mixin is active");
        System.out.println("packName in constructor: " + packName);
    }

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
        // Apply the color transformation for Complementary shaders
        if (name.matches(".*Complementary.*")) {
            String originalName = name;
            String modifiedName = name.replaceFirst("Complementary", "§cComplementary§r");
            System.out.println("Legacy mixin modifying name: Before [" + originalName + "] -> After [" + modifiedName + "]");
            return modifiedName;
        }
        return name;
    }
}