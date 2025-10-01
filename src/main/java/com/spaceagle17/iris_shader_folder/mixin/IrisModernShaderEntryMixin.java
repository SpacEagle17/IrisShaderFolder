package com.spaceagle17.iris_shader_folder.mixin;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderRecolorSystem;

import com.spaceagle17.iris_shader_folder.ShaderTooltipSystem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.irisshaders.iris.gui.element.ShaderPackSelectionList$ShaderPackEntry", remap = false)
public class IrisModernShaderEntryMixin {
    @Unique
    private String currentShaderName;

    @Unique
    private String currentShaderNameRecolored;
    
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
        this.currentShaderName = name;
        this.currentShaderNameRecolored = recoloredName;
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
            if (!isCurrentlyHovered || currentShaderName == null) {
                return;
            }

            // Check if we have a tooltip for this shader
            String tooltip = ShaderTooltipSystem.getInstance().getTooltip(currentShaderName);
            
            // Only proceed if we have a tooltip
            if (tooltip != null && !tooltip.isEmpty()) {
                // Get the screen object through reflection
                Field listField = this.getClass().getDeclaredField("list");
                listField.setAccessible(true);
                Object listObj = listField.get(this);

                Field screenField = listObj.getClass().getDeclaredField("screen");
                screenField.setAccessible(true);
                Object screen = screenField.get(listObj);

                // Create text components for the tooltip
                Object[] components = irisShaderFolder$createTextComponents(currentShaderNameRecolored, tooltip);
                if (components == null) {
                    return;
                }

                // Set the shader pack comment
                boolean success = irisShaderFolder$setShaderPackComment(screen, components[0], components[1]);

                if (!success && IrisShaderFolder.debugLoggingEnabled) {
                    System.out.println("Could not find an appropriate method to set shader pack comment");
                }
            }
        } catch (Exception e) {
            if (IrisShaderFolder.debugLoggingEnabled) {
                System.out.println("Error in shader tooltip handling: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates text components using various approaches
     * @return Array with [title, body] components or null if all approaches failed
     */
    @Unique
    private Object[] irisShaderFolder$createTextComponents(String title, String body) {
        // Define known classes and methods
        String[][] approaches = {
                // className, methodName
                {"net.minecraft.class_2561", "method_43470"}, // Fabric modern
                {"net.minecraft.network.chat.Component", "literal"}, // Forge/NeoForge modern
                {"net.minecraft.network.chat.Component", "m_237113_"}, // 1.20.1 Mojang mappings
                {"net.minecraft.network.chat.Component", "m_130674_"}, // 1.18.2 Mojang mappings
        };

        // Constructor approaches as fallback
        String[] constructorClasses = {
                "net.minecraft.class_2585", // Fabric old
                "net.minecraft.network.chat.TextComponent", // Forge old
                "net.minecraft.util.text.StringTextComponent" // Very old Forge
        };

        // Try each method approach
        for (String[] approach : approaches) {
            try {
                Class<?> componentClass = Class.forName(approach[0]);
                Method method = componentClass.getMethod(approach[1], String.class);
                Object titleComponent = method.invoke(null, title);
                Object bodyComponent = method.invoke(null, body);
                return new Object[]{titleComponent, bodyComponent};
            } catch (Exception ignored) {
                // Try next approach
            }
        }

        // Try constructor approaches
        for (String className : constructorClasses) {
            try {
                Class<?> componentClass = Class.forName(className);
                Object titleComponent = componentClass.getConstructor(String.class).newInstance(title);
                Object bodyComponent = componentClass.getConstructor(String.class).newInstance(body);
                return new Object[]{titleComponent, bodyComponent};
            } catch (Exception ignored) {
                // Try next approach
            }
        }

        // If all approaches failed, try reflection
        try {
            // Find any component class we can use
            Class<?> componentClass = null;
            for (String className : new String[]{
                    "net.minecraft.network.chat.Component",
                    "net.minecraft.text.Text",
                    "net.minecraft.class_2561"
            }) {
                try {
                    componentClass = Class.forName(className);
                    break;
                } catch (ClassNotFoundException ignored) {
                    // Try next class
                }
            }

            if (componentClass != null) {
                // Find any static method with String parameter
                for (Method m : componentClass.getMethods()) {
                    if (m.getParameterCount() == 1 &&
                            m.getParameterTypes()[0] == String.class &&
                            java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        try {
                            Object titleComponent = m.invoke(null, title);
                            Object bodyComponent = m.invoke(null, body);
                            return new Object[]{titleComponent, bodyComponent};
                        } catch (Exception ignored) {
                            // Try next method
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Reflection approach failed
        }

        if (IrisShaderFolder.debugLoggingEnabled) {
            System.out.println("Failed to create text components - cannot set shader pack comment");
        }
        return null;
    }
    /**
     * Attempts to find and invoke the setShaderPackComment method on the screen object
     * @return true if successful, false otherwise
     */
    @Unique
    private boolean irisShaderFolder$setShaderPackComment(Object screen, Object title, Object body) {
        try {
            // First try with exact method name
            try {
                Method method = screen.getClass().getDeclaredMethod("setShaderPackComment", title.getClass(), body.getClass());
                method.setAccessible(true);
                method.invoke(screen, title, body);
                return true;
            } catch (Exception ignored) {
                // Try the approach that worked in your original code
                for (Method method : screen.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() == 2) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        // This pattern matching worked in your original code
                        if (paramTypes[0].getSimpleName().contains("Component") ||
                                paramTypes[0].getName().contains("chat") ||
                                paramTypes[0].getName().contains("text")) {
                            method.setAccessible(true);
                            try {
                                method.invoke(screen, title, body);
                                return true;
                            } catch (Exception e) {
                                // This specific method failed, continue to the next one
                            }
                        }
                    }
                }

                // Last resort - try any method with 2 parameters
                for (Method method : screen.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() == 2) {
                        method.setAccessible(true);
                        try {
                            method.invoke(screen, title, body);
                            return true;
                        } catch (Exception e) {
                            // Continue to the next method
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // All approaches failed
        }

        return false;
    }
}