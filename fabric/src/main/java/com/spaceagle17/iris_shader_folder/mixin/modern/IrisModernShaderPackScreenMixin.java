package com.spaceagle17.iris_shader_folder.mixin.modern;

import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.irisshaders.iris.gui.screen.ShaderPackScreen", remap = false)
public abstract class IrisModernShaderPackScreenMixin {
    @Shadow private Optional<?> hoveredElementCommentTitle;
    @Shadow private List<?> hoveredElementCommentBody;
    @Shadow private int hoveredElementCommentTimer;

    @Unique private static final String[] FONT_FIELD_NAMES = {"font", "field_22793"};
    @Unique private static final String[] VISUAL_ORDER_METHODS = {"getVisualOrderText", "method_30937"};
    @Unique private static final String[] FONT_SPLIT_METHODS = {"split", "method_1728"};

    @Unique
    public void setShaderPackComment(Object title, Object body) {
        if (title == null || body == null) {
            return;
        }

        List<?> splitBody = irisShaderFolder$splitBodyForComment(body);
        if (splitBody == null || splitBody.isEmpty()) {
            Object visualOrderText = irisShaderFolder$toVisualOrderText(body);
            if (visualOrderText == null) {
                return;
            }
            this.hoveredElementCommentBody = new ArrayList<>(Collections.singletonList(visualOrderText));
        } else {
            this.hoveredElementCommentBody = new ArrayList<>(splitBody);
        }

        this.hoveredElementCommentTitle = Optional.of(title);
        this.hoveredElementCommentTimer = 21;
    }

    @Unique
    private List<?> irisShaderFolder$splitBodyForComment(Object body) {
        Object font = irisShaderFolder$findFontObject();
        if (font == null) {
            return null;
        }

        Method splitMethod = null;
        for (Method method : font.getClass().getMethods()) {
            if (!irisShaderFolder$matchesAny(method.getName(), FONT_SPLIT_METHODS)) {
                continue;
            }

            splitMethod = method;
            break;
        }

        if (splitMethod == null) {
            return null;
        }

        try {
            Object result = splitMethod.invoke(font, body, 306);
            if (result instanceof List<?>) {
                return (List<?>) result;
            }
        } catch (Throwable ignored) {
            // Fall back to visual-order text in caller.
        }

        return null;
    }

    @Unique
    private Object irisShaderFolder$findFontObject() {
        Class<?> currentClass = this.getClass();
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (!irisShaderFolder$matchesAny(field.getName(), FONT_FIELD_NAMES)) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object fontValue = field.get(this);
                    if (fontValue != null) {
                        return fontValue;
                    }
                } catch (Throwable ignored) {
                    // Continue scanning known candidates.
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

    @Unique
    private Object irisShaderFolder$toVisualOrderText(Object body) {
        for (String methodName : VISUAL_ORDER_METHODS) {
            try {
                return body.getClass().getMethod(methodName).invoke(body);
            } catch (Throwable ignored) {
                // Try next known method name.
            }
        }

        return null;
    }

    @Unique
    private boolean irisShaderFolder$matchesAny(String value, String[] candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
