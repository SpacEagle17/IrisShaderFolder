package com.spaceagle17.iris_shader_folder.forge.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class IrisShaderFolderMixinPlugin implements IMixinConfigPlugin {
    public static final String LEGACY_IRIS_CLASS = "net.coderbot.iris.gui.element.ShaderPackSelectionList";
    public static final String MODERN_IRIS_CLASS = "net.irisshaders.iris.gui.element.ShaderPackSelectionList";

    @Override
    public void onLoad(String mixinPackage) {
        // No initialization needed
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("IrisLegacyHideShaderPacksMixin") ||
                mixinClassName.contains("IrisLegacyShaderEntryMixin") ||
                mixinClassName.contains("IrisLegacyOptionMenuConstructorMixin") ||
                mixinClassName.contains("IrisLegacyShaderPackScreenMixin")) {
            return checkClassExists(LEGACY_IRIS_CLASS);
        }

        if (mixinClassName.contains("IrisModernHideShaderPacksMixin") ||
                mixinClassName.contains("IrisModernShaderEntryMixin") ||
                mixinClassName.contains("IrisModernOptionMenuConstructorMixin") ||
                mixinClassName.contains("IrisModernShaderPackScreenMixin")) {
            return checkClassExists(MODERN_IRIS_CLASS);
        }

        // Apply other mixins by default
        return true;
    }

    private boolean checkClassExists(String className) {
        String resourceName = className.replace('.', '/') + ".class";
        return getClass().getClassLoader().getResource(resourceName) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // Not needed
    }

    @Override
    public List<String> getMixins() {
        return null; // Return mixins defined in the JSON
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Not needed
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Not needed
    }
}
