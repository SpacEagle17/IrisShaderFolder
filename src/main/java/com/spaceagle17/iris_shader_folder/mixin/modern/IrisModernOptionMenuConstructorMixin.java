package com.spaceagle17.iris_shader_folder.mixin.modern;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderRecolorSystem;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
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
        String recoloredName = ShaderRecolorSystem.getInstance().recolorShaderName(originalPackName);
        if (IrisShaderFolder.debugLoggingEnabled) {
            System.out.println("[IrisShaderFolder] Recolored pack name: " + originalPackName + " -> " + recoloredName);
        }
        return recoloredName;
    }
}