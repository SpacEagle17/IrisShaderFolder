package com.spaceagle17.iris_shader_folder.forge;

import com.spaceagle17.iris_shader_folder.forge.util.ShaderPatternUtil;
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
					IrisShaderFolder.LOGGER.error("Invalid rename rule format: {}", trimmedRule);
					continue;
				}

				String shaderPattern = parts[0].trim();
				List<RenamePartRule> partRules = new ArrayList<>();

				for (int i = 1; i < parts.length; i++) {
					String[] renameParts = parts[i].split("\\s*\\[->\\]\\s*", 2);
					if (renameParts.length != 2) {
						IrisShaderFolder.LOGGER.error("Invalid rename part rule format: {}", parts[i]);
						continue;
					}

					String partPattern = renameParts[0].trim();
					String replacement = decodeReplacement(renameParts[1].trim());
					partRules.add(new RenamePartRule(partPattern, replacement));
				}

				if (!partRules.isEmpty()) {
					renameRules.add(new RenameRule(shaderPattern, partRules));
					ShaderPatternUtil.logDebug("Added rename rule for pattern: " + shaderPattern +
						" with " + partRules.size() + " replacement rule(s)");
				}
			} catch (Exception e) {
				IrisShaderFolder.LOGGER.error("Error parsing rename rule: " + trimmedRule, e);
			}
		}
	}

	public String renameShaderName(String name) {
		if (renameCache.containsKey(name)) {
			return renameCache.get(name);
		}

		String result = name;
		boolean modified = false;

		ShaderPatternUtil.logDebug("Processing shader rename: [" + name + "]");
		ShaderPatternUtil.logDebug("--- Applying rename rules ---");

		for (RenameRule rule : renameRules) {
			ShaderPatternUtil.logDebug("- Checking rename rule with shader pattern: [" + rule.getShaderPattern() + "]");

			if (!ShaderPatternUtil.matchesPattern(name, rule.getShaderPattern())) {
				ShaderPatternUtil.logDebug("  - Rule does not match");
				continue;
			}

			ShaderPatternUtil.logDebug("  - Rule matches!");
			for (RenamePartRule partRule : rule.getPartRules()) {
				String before = result;
				result = applyRenamePartRule(result, partRule);

				if (!before.equals(result)) {
					modified = true;
					ShaderPatternUtil.logDebug("  - Applied rename rule [" + partRule.getPartPattern() +
						" -> " + partRule.getReplacement() + "]");
					ShaderPatternUtil.logDebug("    * Before: [" + before + "]");
					ShaderPatternUtil.logDebug("    * After:  [" + result + "]");
				} else {
					ShaderPatternUtil.logDebug("  - Rename rule [" + partRule.getPartPattern() + "] had no effect");
				}
			}
		}

		renameCache.put(name, result);

		if (modified) {
			ShaderPatternUtil.logDebug("=== FINAL RENAME RESULT ===");
			ShaderPatternUtil.logDebug("- Original: [" + name + "]");
			ShaderPatternUtil.logDebug("- Renamed:  [" + result + "]");
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
			IrisShaderFolder.LOGGER.error("Invalid pattern in rename rule: " + partPattern, e);
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

		public String getShaderPattern() {
			return shaderPattern;
		}

		public List<RenamePartRule> getPartRules() {
			return partRules;
		}
	}

	private static class RenamePartRule {
		private final String partPattern;
		private final String replacement;

		public RenamePartRule(String partPattern, String replacement) {
			this.partPattern = partPattern;
			this.replacement = replacement;
		}

		public String getPartPattern() {
			return partPattern;
		}

		public String getReplacement() {
			return replacement;
		}
	}
}
