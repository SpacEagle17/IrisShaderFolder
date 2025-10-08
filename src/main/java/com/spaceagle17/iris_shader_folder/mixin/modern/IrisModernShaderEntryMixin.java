package com.spaceagle17.iris_shader_folder.mixin.modern;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.ShaderRecolorSystem;
import com.spaceagle17.iris_shader_folder.ShaderSwapNameSystem;

import com.spaceagle17.iris_shader_folder.ShaderTooltipSystem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
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
    
    @Unique
    private static boolean componentMethodInitialized = false;
    @Unique
    private static Method cachedComponentMethod = null;
    @Unique
    private static Class<?> cachedComponentClass = null;
    @Unique
    private static boolean useConstructor = false;
    @Unique
    private static Constructor<?> cachedConstructor = null;

    @Unique
    private static boolean commentMethodInitialized = false;
    @Unique
    private static Method cachedCommentMethod = null;
    
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
        // Apply name swapping first
        String swappedName = ShaderSwapNameSystem.getInstance().swapShaderName(name);
        // Then apply recoloring
        String recoloredName = ShaderRecolorSystem.getInstance().recolorShaderName(swappedName);
        this.currentShaderName = name; // Store the original name for tooltips
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
        // Check if we already found a working method
        if (componentMethodInitialized && cachedComponentClass != null) {
            try {
                if (useConstructor && cachedConstructor != null) {
                    // Use cached constructor
                    Object titleComponent = cachedConstructor.newInstance(title);
                    Object bodyComponent = cachedConstructor.newInstance(body);
                    return new Object[]{titleComponent, bodyComponent};
                } else if (cachedComponentMethod != null) {
                    // Use cached method
                    Object titleComponent = cachedComponentMethod.invoke(null, title);
                    Object bodyComponent = cachedComponentMethod.invoke(null, body);
                    return new Object[]{titleComponent, bodyComponent};
                }
            } catch (Exception ignored) {
                // Fall back to searching again if cached approach fails
                componentMethodInitialized = false;
            }
        }
        
        // If not initialized or cached approach failed, try to find a working method
        
        // Define known classes and methods
        String[][] approaches = {
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
                
                // Cache the successful method
                cachedComponentClass = componentClass;
                cachedComponentMethod = method;
                useConstructor = false;
                componentMethodInitialized = true;
                
                return new Object[]{titleComponent, bodyComponent};
            } catch (Exception ignored) {
                // Try next approach
            }
        }

        // Try constructor approaches
        for (String className : constructorClasses) {
            try {
                Class<?> componentClass = Class.forName(className);
                Constructor<?> constructor = componentClass.getConstructor(String.class);
                Object titleComponent = constructor.newInstance(title);
                Object bodyComponent = constructor.newInstance(body);
                
                // Cache the successful constructor
                cachedComponentClass = componentClass;
                cachedConstructor = constructor;
                useConstructor = true;
                componentMethodInitialized = true;
                
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
        // Check if we already found a working method
        if (commentMethodInitialized && cachedCommentMethod != null) {
            try {
                cachedCommentMethod.invoke(screen, title, body);
                return true;
            } catch (Exception ignored) {
                // Fall back to searching again if cached method fails
                commentMethodInitialized = false;
            }
        }
        
        try {
            // First try with exact method name
            try {
                Method method = screen.getClass().getDeclaredMethod("setShaderPackComment", title.getClass(), body.getClass());
                method.setAccessible(true);
                method.invoke(screen, title, body);
                
                // Cache the successful method
                cachedCommentMethod = method;
                commentMethodInitialized = true;
                
                return true;
            } catch (Exception ignored) {
                for (Method method : screen.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() == 2) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes[0].getSimpleName().contains("Component") ||
                                paramTypes[0].getName().contains("chat") ||
                                paramTypes[0].getName().contains("text")) {
                            method.setAccessible(true);
                            try {
                                method.invoke(screen, title, body);
                                
                                // Cache the successful method
                                cachedCommentMethod = method;
                                commentMethodInitialized = true;
                                
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
                            
                            // Cache the successful method
                            cachedCommentMethod = method;
                            commentMethodInitialized = true;
                            
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