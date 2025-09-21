# Iris Shader Folder Mod
A mod that allows you to hide or filter out specific shader packs from the Iris shaders menu using flexible patterns.

## Features
- Hide shader packs by name, version, or regex pattern
- Supports both modern and legacy Iris versions
- Real-time config reloading (no restart required)
- Debug logging to file for troubleshooting
## Configuration
Edit the config file at:
`config/iris_shader_folder.properties`
### Example filter section:
```
filterStart:[
test
Euphoria{.{1,20}}
ComplementaryShaders_r{version}
]:filterEnd
```

- Each line is a filter pattern.
- Use {version} as a shortcut for version numbers (e.g., 1.2.3).
- Use {...} to insert raw regex (e.g., Euphoria{.{1,20}}).
- Patterns match both folders and .zip shader packs.
Enable Debug Logging
Set `debugLogging=true` in the config file to write detailed debug info to `config/iris_shader_filter_debug.txt`