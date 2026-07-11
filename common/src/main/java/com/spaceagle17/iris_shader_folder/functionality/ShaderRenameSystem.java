package com.spaceagle17.iris_shader_folder.functionality;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import com.spaceagle17.iris_shader_folder.config.ConfigManager;
import com.spaceagle17.iris_shader_folder.util.ShaderPatternUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ShaderRenameSystem implements ConfigManager.ConfigUpdateListener {
	private static ShaderRenameSystem INSTANCE;

	private final Map<String, String> renameCache = new HashMap<>();
	private List<RenameRule> renameRules = new ArrayList<>();
	private List<String> lastRenamePatterns = new ArrayList<>();

	private ShaderRenameSystem() {
		ConfigManager.registerUpdateListener(this);
		updateRules();
	}

	private static void debugLog(String message) {
		IrisShaderFolder.debugLog("[ShaderRenameSystem]" + message);
	}

	public static ShaderRenameSystem getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ShaderRenameSystem();
		}
		return INSTANCE;
	}

	public void updateRules() {
		List<String> configRenamePatterns = IrisShaderFolder.getInstance().getRenamePatterns();
		if (configRenamePatterns.equals(lastRenamePatterns)) {
			return;
		}

		lastRenamePatterns = new ArrayList<>(configRenamePatterns);
		renameRules = new ArrayList<>();
		renameCache.clear();

		for (String rule : configRenamePatterns) {
			String trimmedRule = rule.trim();
			if (trimmedRule.isEmpty() || trimmedRule.startsWith("#")) {
				continue;
			}

			try {
				String[] parts = trimmedRule.split("\\s*\\[\\|\\]\\s*");
				if (parts.length < 2) {
					IrisShaderFolder.log(3, "Invalid rename rule format: " + trimmedRule);
					continue;
				}

				String shaderPattern = parts[0].trim();
				List<RenamePartRule> partRules = new ArrayList<>();

				for (int i = 1; i < parts.length; i++) {
					String[] renameParts = parts[i].split("\\s*\\[->\\]\\s*", 2);
					if (renameParts.length != 2) {
						IrisShaderFolder.log(3, "Invalid rename part rule format: " + parts[i]);
						continue;
					}

					String partPattern = renameParts[0].trim();
					String replacement = decodeReplacement(renameParts[1].trim());
					partRules.add(new RenamePartRule(partPattern, replacement));
				}

				if (!partRules.isEmpty()) {
					renameRules.add(new RenameRule(shaderPattern, partRules));
					debugLog("Added rename rule for pattern: " + shaderPattern +
						" with " + partRules.size() + " replacement rule(s)");
				}
			} catch (Exception e) {
				IrisShaderFolder.log(3, "Error parsing rename rule: " + trimmedRule + ": " + e.getMessage());
			}
		}
	}

	public String renameShaderName(String name) {
		if (renameCache.containsKey(name)) {
			return renameCache.get(name);
		}

		String result = name;
		boolean modified = false;

		debugLog("Processing shader rename: [" + name + "]");
		debugLog("--- Applying rename rules ---");

		for (RenameRule rule : renameRules) {
			debugLog("- Checking rename rule with shader pattern: [" + rule.getShaderPattern() + "]");

			if (!ShaderPatternUtil.matchesPattern(name, rule.getShaderPattern())) {
				debugLog("  - Rule does not match");
				continue;
			}

			debugLog("  - Rule matches!");
			for (RenamePartRule partRule : rule.getPartRules()) {
				String before = result;
				result = applyRenamePartRule(result, partRule);

				if (!before.equals(result)) {
					modified = true;
					debugLog("  - Applied rename rule [" + partRule.getPartPattern() +
						" -> " + partRule.getReplacement() + "]");
					debugLog("    * Before: [" + before + "]");
					debugLog("    * After:  [" + result + "]");
				} else {
					debugLog("  - Rename rule [" + partRule.getPartPattern() + "] had no effect");
				}
			}
		}

		renameCache.put(name, result);

		if (modified) {
			debugLog("=== FINAL RENAME RESULT ===");
			debugLog("- Original: [" + name + "]");
			debugLog("- Renamed:  [" + result + "]");
		}

		return result;
	}

	private String applyRenamePartRule(String input, RenamePartRule partRule) {
		String partPattern = partRule.getPartPattern();
		String replacement = partRule.getReplacement();

		if ("{all}".equals(partPattern)) {
			return replacement;
		}

		try {
			String regexPattern = ShaderPatternUtil.convertToRegex(partPattern);
			Pattern compiledPattern = Pattern.compile(regexPattern);
			Matcher matcher = compiledPattern.matcher(input);
			return matcher.replaceAll(Matcher.quoteReplacement(replacement));
		} catch (PatternSyntaxException e) {
			IrisShaderFolder.log(3, "Invalid pattern in rename rule: " + partPattern + ": " + e.getMessage());
			return input;
		}
	}

	private String decodeReplacement(String replacement) {
		return replacement.replace("{ }", " ");
	}

	@Override
	public void onConfigUpdate() {
		updateRules();
	}

	public void clearCache() {
		renameCache.clear();
	}

	private static class RenameRule {
		private final String shaderPattern;
		private final List<RenamePartRule> partRules;

		public RenameRule(String shaderPattern, List<RenamePartRule> partRules) {
			this.shaderPattern = shaderPattern;
			this.partRules = partRules;
		}

		public String getShaderPattern() { return shaderPattern; }
		public List<RenamePartRule> getPartRules() { return partRules; }
	}

	private static class RenamePartRule {
		private final String partPattern;
		private final String replacement;

		public RenamePartRule(String partPattern, String replacement) {
			this.partPattern = partPattern;
			this.replacement = replacement;
		}

		public String getPartPattern() { return partPattern; }
		public String getReplacement() { return replacement; }
	}
}
