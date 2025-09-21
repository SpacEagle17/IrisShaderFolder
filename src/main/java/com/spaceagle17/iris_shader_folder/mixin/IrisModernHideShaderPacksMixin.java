package com.spaceagle17.iris_shader_folder.mixin;

import com.spaceagle17.iris_shader_folder.ConfigManager;
import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderFilterSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.stream.Collectors;

@Pseudo
@Mixin(targets = IrisShaderFolderMixinPlugin.MODERN_IRIS_CLASS, remap = false)
public class IrisModernHideShaderPacksMixin {
    @ModifyVariable(
        method = "refresh()V",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0,
        remap = false
    )
    private List<String> filterShaderPacks(List<String> names) {
        ShaderFilterSystem filterSystem = ShaderFilterSystem.getInstance();
        if (ConfigManager.getInstance().isDebugLoggingEnabled()) {
            IrisShaderFolder.LOGGER.info("This is Iris Modern!!");
        }
        return names.stream()
            .filter(filterSystem::shouldFilterShaderPack)
            .collect(Collectors.toList());
    }
}