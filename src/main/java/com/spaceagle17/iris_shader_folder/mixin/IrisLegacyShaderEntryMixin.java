package com.spaceagle17.iris_shader_folder.mixin;

import com.spaceagle17.iris_shader_folder.ShaderRecolorSystem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.coderbot.iris.gui.element.ShaderPackSelectionList$ShaderPackEntry", remap = false)
public class IrisLegacyShaderEntryMixin {
    @Unique
    private String currentShaderName;

    @Unique
    private boolean isCurrentlyHovered;

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
        String recoloredName = ShaderRecolorSystem.getInstance().recolorShaderName(name);
        this.currentShaderName = recoloredName;
        return recoloredName;
    }

    @ModifyVariable(
            method = {
                    "render",
                    "renderContent",
                    "method_25343",
                    "m_6311_"
            },
            at = @At("HEAD"),
            ordinal = 0,
            name = "isHovered",
            remap = false
    )
    private boolean captureIsHovered(boolean isHovered) {
        this.isCurrentlyHovered = isHovered;
        return isHovered;
    }

    @Inject(
            method = {
                    "render",
                    "renderContent",
                    "method_25343",
                    "m_6311_"
            },
            at = @At("TAIL"))
    private void afterRenderText(CallbackInfo ci) {
        try {
            if (isCurrentlyHovered && currentShaderName != null && currentShaderName.contains("Euphoria-Patches")) {
                Field listField = this.getClass().getDeclaredField("list");
                listField.setAccessible(true);
                Object listObj = listField.get(this);

                Field screenField = listObj.getClass().getDeclaredField("screen");
                screenField.setAccessible(true);
                Object screen = screenField.get(listObj);

                Object commentTitle = null;
                Object commentBody = null;

                String bodyText = "A Complementary Shaders Add-on - By SpacEagle17. Dev versions available at: §dhttps://euphoriapatches.com/support";

                // Try multiple approaches to create text components based on what's available in the current environment

                // 1. Try Fabric obfuscated names
                try {
                    Class<?> componentClass = Class.forName("net.minecraft.class_2561");
                    commentTitle = componentClass.getMethod("method_43470", String.class)
                            .invoke(null, currentShaderName);
                    commentBody = componentClass.getMethod("method_43470", String.class)
                            .invoke(null, bodyText);
                } catch (Exception e) {
                    // 2. Try older Fabric obfuscated names (TextComponent constructor)
                    try {
                        Class<?> textComponentClass = Class.forName("net.minecraft.class_2585");
                        commentTitle = textComponentClass.getConstructor(String.class)
                                .newInstance(currentShaderName);
                        commentBody = textComponentClass.getConstructor(String.class)
                                .newInstance(bodyText);
                    } catch (Exception e2) {
                        // 3. Try NeoForge/Forge names (Component.literal)
                        try {
                            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
                            commentTitle = componentClass.getMethod("literal", String.class)
                                    .invoke(null, currentShaderName);
                            commentBody = componentClass.getMethod("literal", String.class)
                                    .invoke(null, bodyText);
                        } catch (Exception e3) {
                            // 4. Try older Forge names (TextComponent constructor)
                            try {
                                Class<?> textComponentClass = Class.forName("net.minecraft.network.chat.TextComponent");
                                commentTitle = textComponentClass.getConstructor(String.class)
                                        .newInstance(currentShaderName);
                                commentBody = textComponentClass.getConstructor(String.class)
                                        .newInstance(bodyText);
                            } catch (Exception e4) {
                                // If all attempts failed, print all exceptions for debugging
                                System.out.println("Failed to create components using Fabric obfuscated names: " + e.getMessage());
                                System.out.println("Failed to create components using older Fabric names: " + e2.getMessage());
                                System.out.println("Failed to create components using Forge names: " + e3.getMessage());
                                System.out.println("Failed to create components using older Forge names: " + e4.getMessage());
                            }
                        }
                    }
                }

                for (java.lang.reflect.Method method : screen.getClass().getDeclaredMethods()) {
                    if (method.getName().equals("setShaderPackComment") && method.getParameterCount() == 2) {
                        method.setAccessible(true);
                        method.invoke(screen, commentTitle, commentBody);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error in Euphoria comment handling: " + e.getMessage());
            e.printStackTrace();
        }
    }
}