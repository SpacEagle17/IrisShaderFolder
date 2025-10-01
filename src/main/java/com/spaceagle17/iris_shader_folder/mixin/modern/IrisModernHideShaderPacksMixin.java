package com.spaceagle17.iris_shader_folder.mixin.modern;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderFilterSystem;
import com.spaceagle17.iris_shader_folder.ShaderReorderSystem;
import com.spaceagle17.iris_shader_folder.mixin.IrisShaderFolderMixinPlugin;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.stream.Collectors;

@Pseudo
@Debug(export = true)
@Mixin(targets = IrisShaderFolderMixinPlugin.MODERN_IRIS_CLASS, remap = false)
public class IrisModernHideShaderPacksMixin {
    @ModifyVariable(
        method = "refresh()V",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0,
        remap = false
    )
    private List<String> filterAndReorderShaderPacks(List<String> names) {
        ShaderFilterSystem filterSystem = ShaderFilterSystem.getInstance();
        ShaderReorderSystem reorderSystem = ShaderReorderSystem.getInstance();
        
        if (IrisShaderFolder.debugLoggingEnabled) {
            System.out.println("This is Iris Modern!!");
        }
        
        // First filter the packs
        List<String> filteredPacks = names.stream()
            .filter(filterSystem::shouldFilterShaderPack)
            .collect(Collectors.toList());
        
        // Then reorder them
        return reorderSystem.reorderShaderPacks(filteredPacks);
    }
}