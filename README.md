# Iris Shader Folder Mod
A mod that allows you to hide, filter, and reorder specific shader packs in the Iris shaders menu using flexible patterns.

## 📝 Why use this mod?
Tired of scrolling through a messy shader list? Want to hide test packs, old versions, or put your favorites at the top? Iris Shader Folder Mod lets you filter out unwanted shader packs and organize the menu exactly how you want—making your shader selection fast, clean, and personal.

## Features
- Hide shader packs by name, version, or regex pattern
- Reorder shader packs in the menu using patterns and positions
- Supports both modern and legacy **Iris** versions and also **Oculus**
- Real-time config reloading (no restart required)
- Debug logging to file for troubleshooting

## Configuration
Edit the config file at:
`config/iris_shader_folder.properties`

### Example filter section:
codeblockStart
filterStart:[
test
{.*}Outdated{.*}
ComplementaryShaders_r{version}
]:filterEnd
codeBlockEnd

- Each line is a filter pattern.
- Use {version} as a shortcut for version numbers (e.g., 1.2.3).
- Use {...} to insert raw regex (e.g., `Outdated{.*(shader|Reimagined)}`).
- Patterns match both folders and .zip shader packs.

### Example reorder section:
```
reorderStart:[
{.*}EuphoriaPatches{.*} -> 1
Complementary{.*}_r{version} -> 2
BSL{.*} -> 3
]:reorderEnd
```

- Each line is `<pattern> -> <position>`, where position is 1-based (1 = first slot).
- If multiple shaderpacks match a pattern, they are inserted at the given position, sorted alphabetically.
- Patterns support {version} and custom regex in braces.
- Patterns match both folders and .zip shader packs.

### Enable Debug Logging
Set `debugLogging=true` in the config file to write detailed debug info to `config/iris_shader_filter_debug.txt`
