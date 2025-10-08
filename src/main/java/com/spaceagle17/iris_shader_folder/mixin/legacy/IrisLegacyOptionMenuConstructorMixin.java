package com.spaceagle17.iris_shader_folder.mixin.legacy;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderRecolorSystem;
import com.spaceagle17.iris_shader_folder.ShaderSwapNameSystem;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.coderbot.iris.gui.element.widget.OptionMenuConstructor", remap = false)
public class IrisLegacyOptionMenuConstructorMixin {

    /**
     * Modifies the expression value of getCurrentPackName() within the lambda method
     */
    @ModifyExpressionValue(
        method = "lambda$static$3(Lnet/coderbot/iris/shaderpack/option/menu/OptionMenuMainElementScreen;)Lnet/coderbot/iris/gui/element/screen/ElementWidgetScreenData;",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/coderbot/iris/Iris;getCurrentPackName()Ljava/lang/String;",
            remap = false
        ),
        remap = false
    )
   private static String modifyPackNameValue(String originalPackName) {
        // Apply name swapping first
        String swappedName = ShaderSwapNameSystem.getInstance().swapShaderName(originalPackName);
        // Then apply recoloring
        String recoloredName = ShaderRecolorSystem.getInstance().recolorShaderName(swappedName);
        if (IrisShaderFolder.debugLoggingEnabled) {
            System.out.println("[IrisShaderFolder] Modified pack name: " + originalPackName + 
                " → " + swappedName + " → " + recoloredName);
        }
        return recoloredName;
    }
}