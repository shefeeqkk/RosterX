# Versioning Guidelines

Whenever performing modifications and applying changes to RosterX:
- **Small Update / Patch**: Increment the patch version (e.g., `1.0.0` -> `1.0.1`).
- **Semi-Big Update / Minor Feature**: Increment the middle (minor) version (e.g., `1.0.1` -> `1.1.0`).
- **Major Update**: Increment the major version (e.g., `1.1.0` -> `2.0.0`).

Always reflect version updates in:
1. Android project: `C:\Users\hp\AndroidStudioProjects\RosterX\app\build.gradle.kts` (`versionName` and `versionCode`).
2. Server backend: `c:\shefeeq\PYTHON\RosterX\server.py` (`SERVER_VERSION`).
3. Documentation and release notes.
