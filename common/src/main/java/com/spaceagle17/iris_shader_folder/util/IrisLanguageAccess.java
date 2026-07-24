package com.spaceagle17.iris_shader_folder.util;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reflectively retrieves Iris's resolved language fallback order.
 * Piggybacks on the locale list injected into vanilla's language system by Iris.
 */
public final class IrisLanguageAccess {
    private IrisLanguageAccess() {}

    private static final List<String> FALLBACK = Collections.singletonList("en_us");

    private static Object languageInstance = null;
    private static boolean reflectionFailed = false;

    private static Field languageCodesField = null;
    private static boolean languageCodesFieldLookupFailed = false;

    static {
        try {
            Class<?> languageClass = null;
            for (String name : new String[]{"net.minecraft.locale.Language", "net.minecraft.class_2477"}) {
                try {
                    languageClass = Class.forName(name);
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (languageClass != null) {
                for (String name : new String[]{"getInstance", "method_10517", "m_128107_"}) {
                    try {
                        Method method = languageClass.getMethod(name);
                        Object result = method.invoke(null);
                        if (result != null) {
                            languageInstance = result;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable t) {
            debugLog("Static reflection initialization failed: " + t);
        }

        if (languageInstance == null) {
            reflectionFailed = true;
        }
    }

    /**
     * Returns the ordered list of language codes Iris resolved for the active game locale
     * (e.g. {@code ["pl_pl", "en_us"]}), or {@code ["en_us"]} if Iris isn't loaded or the
     * field can't be read.
     */
    public static List<String> getActiveLanguageCodes() {
        if (reflectionFailed || languageCodesFieldLookupFailed) return FALLBACK;

        try {
            if (languageCodesField == null) {
                Class<?> currentClass = languageInstance.getClass();
                while (currentClass != null) {
                    try {
                        languageCodesField = currentClass.getDeclaredField("languageCodes");
                        break;
                    } catch (NoSuchFieldException ignored) {
                        currentClass = currentClass.getSuperclass();
                    }
                }

                if (languageCodesField == null) {
                    languageCodesFieldLookupFailed = true;
                    return FALLBACK;
                }
                languageCodesField.setAccessible(true);
            }

            Object value = languageCodesField.get(null);
            if (value instanceof List) {
                List<String> codes = new ArrayList<>();
                for (Object o : (List<?>) value) {
                    if (o instanceof String) codes.add((String) o);
                }
                if (!codes.isEmpty()) return codes;
            }
        } catch (Throwable t) {
            languageCodesFieldLookupFailed = true;
            debugLog("Failed to read Iris's languageCodes field: " + t);
        }

        return FALLBACK;
    }

    private static void debugLog(String message) {
        IrisShaderFolder.debugLog("[IrisLanguageAccess] " + message);
    }
}
