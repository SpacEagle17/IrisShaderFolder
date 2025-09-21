package com.spaceagle17.iris_shader_folder.mixin;

import com.spaceagle17.iris_shader_folder.ConfigManager;
import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderFilterSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;
import java.util.stream.Collectors;

@Pseudo
@Mixin(targets = IrisShaderFolderMixinPlugin.LEGACY_IRIS_CLASS, remap = false)
public class IrisLegacyHideShaderPacksMixin {
    @ModifyVariable(
        method = "refresh()V",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0,
        remap = false
    )
    private Collection<String> filterShaderPacks(Collection<String> names) {
        ShaderFilterSystem filterSystem = ShaderFilterSystem.getInstance();
        if (ConfigManager.getInstance().isDebugLoggingEnabled()) {
            System.out.println("This is Iris Legacy!!");
        }
        return names.stream()
            .filter(filterSystem::shouldFilterShaderPack)
            .collect(Collectors.toList());
    }
}