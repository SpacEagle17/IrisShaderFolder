package com.spaceagle17.iris_shader_folder.util;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderPatternUtil {

    public static String convertToRegex(String pattern) {
        StringBuilder result = new StringBuilder();
        int currentPos = 0;

        while (currentPos < pattern.length()) {
            int openBrace = pattern.indexOf('{', currentPos);

            if (openBrace == -1) {
                result.append(Pattern.quote(pattern.substring(currentPos)));
                break;
            }

            if (openBrace > currentPos) {
                result.append(Pattern.quote(pattern.substring(currentPos, openBrace)));
            }

            int closeBrace = findMatchingCloseBrace(pattern, openBrace);

            if (closeBrace == -1) {
                result.append(Pattern.quote(pattern.substring(currentPos)));
                break;
            }

            String braceContent = pattern.substring(openBrace + 1, closeBrace);

            if ("version".equals(braceContent)) {
                result.append("\\d+(\\.\\d+)*");
            } else if ("all".equals(braceContent)) {
                result.append(".*");
            } else {
                result.append(braceContent);
            }

            currentPos = closeBrace + 1;
        }

        return result.toString();
    }

    public static int findMatchingCloseBrace(String text, int openBracePos) {
        int depth = 1;

        for (int i = openBracePos + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }

        return -1;
    }

    public static boolean matchesPattern(String text, String patternStr) {
        if (patternStr.trim().isEmpty() || patternStr.startsWith("#")) return false;

        try {
            String regexPattern = convertToRegex(patternStr);
            String finalPattern = "^" + regexPattern + "(\\.zip)?$";
            Pattern pattern = Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(text).matches();
        } catch (PatternSyntaxException e) {
            IrisShaderFolder.log(3, "Invalid pattern: " + patternStr + " - " + e.getMessage());
            return false;
        }
    }
}
