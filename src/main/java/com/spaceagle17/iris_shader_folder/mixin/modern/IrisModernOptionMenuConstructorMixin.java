package com.spaceagle17.iris_shader_folder.mixin.modern;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderRecolorSystem;
import com.spaceagle17.iris_shader_folder.ShaderSwapNameSystem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.irisshaders.iris.gui.element.widget.OptionMenuConstructor", remap = false)
public class IrisModernOptionMenuConstructorMixin {

    /**
     * Modifies the expression value of getCurrentPackName() within the lambda method
     */
    @ModifyExpressionValue(
        method = "lambda$static$0(Lnet/irisshaders/iris/shaderpack/option/menu/OptionMenuMainElementScreen;)Lnet/irisshaders/iris/gui/element/screen/ElementWidgetScreenData;",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/irisshaders/iris/Iris;getCurrentPackName()Ljava/lang/String;",
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