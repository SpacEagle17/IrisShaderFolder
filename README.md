# IRIS/OCULUS ADD-ON. <ins style="color:red;">THIS IS NOT A SHADER LOADER.</ins>

<center>
<div style="display: flex; justify-content: center; flex-wrap: wrap; gap: 8px; text-align: center;">
    <a href="https://www.euphoriapatches.com/discord/" target="_blank">
        <img alt="Discord" src="https://img.shields.io/badge/Discord-050d18?style=for-the-badge&logo=discord&logoColor=%23FF8758&labelColor=FFDB58">
    </a>
    <a href="https://github.com/SpacEagle17/IrisShaderFolder" target="_blank">
        <img alt="Changelogs" src="https://img.shields.io/badge/Source-050d18?style=for-the-badge&logo=github&logoColor=%23FF8758&labelColor=FFDB58">
    </a>
        <img alt="Euphoria Patches" src="https://img.shields.io/badge/Iris%20%7C%20Oculus%20%7C%20and%20derivatives-050d18?style=for-the-badge&logoColor=%23FF8758&label=WORKS%20ON%3A&labelColor=050d18&color=FFDB58">
    <a href="https://www.euphoriapatches.com/support/" target="_blank">
        <img alt="Support" src="https://img.shields.io/badge/Spaceagle17-050d18?style=for-the-badge&logo=patreon&logoColor=%23FF8758&label=Support&labelColor=FFDB58">
    </a>
</div>
<br>
</center>

A mod that lets you hide, filter, recolor, rename, add tooltips, and reorder specific shader packs in the Iris/Oculus shaders menu using flexible patterns.
## This mod does <ins style="color:red;">NOT</ins> load shaders! It only organizes and customizes the shader selection menu.

## 📝 Why use this mod?
Tired of scrolling through a messy shader list? Want to hide test packs, old versions, recolor names, add tooltips, or put your favorites at the top? Or maybe even remove all underscores?
Iris Shader Folder Mod lets you filter out unwanted shader packs, organize the menu, recolor names, and add custom tooltips making your shader selection fast, clean, and personal.

## Features
- Hide shader packs by name, version, or regex pattern
- Reorder shader packs in the menu using patterns and positions
- Recolor shader pack names or parts of names with Minecraft color codes
- Edit shader names without changing the actual file names.
- Add custom tooltips to shader packs in the menu
- Supports both modern and legacy **Iris** versions and also **Oculus**
- Real-time config reloading (no restart required)
- Debug logging to file for troubleshooting

## For Shaderpack Developers
Add your own tooltips easily!
Create a `pack.json` under `/shaders/` (inspired by Aperture) and in json format include:
```json
"description": "Your Description"
```

## Configuration
Edit the config file at:
`config/iris_shader_folder.properties`

### Example filter section:
```
filterStart:[
test
{.*}Outdated{.*}
ComplementaryShaders_r{version}
]:filterEnd
```

- Each line is a filter pattern.
- Use `{version}` as a shortcut for version numbers (e.g., 1.2.3).
- Use `{...}` to insert raw regex (e.g., `Outdated{.*(shader|Reimagined)}`).
- Patterns match both folders and .zip shader packs.

### Example reorder section:
```
reorderStart:[
{.*}EuphoriaPatches{.*}
Complementary{.*}_r{version}
BSL{.*}
[!]Outdated{.*}
]:reorderEnd
```

- The order of lines determines the order in the menu (top line = first position, etc.).
- If a shaderpack matches multiple patterns, the **first matching pattern wins** and subsequent patterns skip that shader.
- Use the `[!]` prefix to force a pattern to match even if the shader was already matched by a previous rule.
  - Example: `[!]Outdated{.*}` will move any shader with "Outdated" in its name to position 4, even if it was already matched by an earlier pattern.
- Patterns support `{version}` and custom regex in braces.
- Patterns match both folders and .zip shader packs.

### Example recolor section:
```
recolorStart:[
Complementary{.*} [|] Comp [->] red [|] {version} [->] §6
{.*}EuphoriaPatches{.*} [|] EuphoriaPatches_{version} [->] light_purple
test [|] {all} [->] red
]:recolorEnd
```

- Recolor the whole name or just parts using Minecraft color names or codes.
- `{all}` recolors the entire name.
- Multiple recolor rules can be combined per line.

### Example rename section:
```
renameStart:[
{all} [|] _ [->] { }
Complementary{.*} [|] Complementary [->] Comp
test [|] {all} [->] Test Shader
]:renameEnd
```

- Rename shader pack names or just parts using flexible patterns.
- `{all}` renames the entire name.
- Use `{ }` to insert a whitespace character as a replacement.
- Format: `shader_pattern [|] part_pattern [->] replacement [|] part_pattern2 [->] replacement2 ...`.
- Multiple rename rules can be combined per line.

### Example tooltip section:
```
tooltipStart:[
Complementary{.*} [|] Complementary is a shaderpack focused on performance and visual quality.
test [|] This is a test shaderpack.
]:tooltipEnd
```

- Add custom tooltip text for any shader pack.
- Tooltips appear when hovering over the shader in the menu.

### Enable Debug Logging
Set `debugLogging=true` in the config file to write detailed debug info to `config/iris_shader_filter_debug.txt`
