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
@Mixin(targets = "net.irisshaders.iris.gui.element.ShaderPackSelectionList$ShaderPackEntry", remap = false)
public class IrisModernShaderEntryMixin {
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

                // First try class_2561.method_43470 (newer MC versions)
                try {
                    Class<?> componentClass = Class.forName("net.minecraft.class_2561");
                    commentTitle = componentClass.getMethod("method_43470", String.class)
                            .invoke(null, currentShaderName);
                    commentBody = componentClass.getMethod("method_43470", String.class)
                            .invoke(null, "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas non vehicula enim. Vestibulum id rhoncus nibh, nec finibus massa. Suspendisse non nisi ultrices, pellentesque tortor et, tincidunt nisl. In hac habitasse platea dictumst. Curabitur venenatis ipsum vel ex eleifend, vitae iaculis leo sodales. Suspendisse ut velit quis libero bibendum imperdiet sit amet iaculis urna. Nullam sodales, libero luctus auctor gravida, turpis turpis lacinia mi, ultrices dictum ipsum risus sed metus. Pellentesque pulvinar eros id leo egestas, nec ultrices felis cursus. Nunc ut tincidunt tellus, vel tempor odio. Donec at dui sit amet ligula viverra vulputate. Morbi sit amet pretium metus. Duis ut urna et tellus malesuada elementum. Suspendisse tristique bibendum pulvinar. Sed ullamcorper libero felis, vel mollis nunc iaculis id. Vestibulum eu nunc at diam pharetra hendrerit a quis turpis.");
                } catch (Exception e) {
                    // If that fails, try class_2585 constructor (older MC versions)
                    try {
                        Class<?> textComponentClass = Class.forName("net.minecraft.class_2585");
                        commentTitle = textComponentClass.getConstructor(String.class)
                                .newInstance(currentShaderName);
                        commentBody = textComponentClass.getConstructor(String.class)
                                .newInstance("A Complementary Shaders Add-on - By SpacEagle17. Dev versions available at: §dhttps://euphoriapatches.com/support");
                    } catch (Exception e2) {
                        throw new RuntimeException("Could not create text components", e2);
                    }
                }

                // Get all methods from the screen class
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
        }
    }
}