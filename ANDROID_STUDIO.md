# Android Studio Installation Details

Android Studio Rabbit 1 (2026.2.1) is installed on this machine for local Android & Kotlin development.

---

## Installation Summary

- **Version**: Android Studio Rabbit 1 | 2026.2.1 (Build `#AI-262.9437.185.2621.16467767`)
- **Installation Path**: `~/.local/android-studio/`
- **CLI Commands**: `studio` and `android-studio`
  - Symlinked to `~/.local/bin/studio` and `~/.local/bin/android-studio`
  - Available system-wide in any terminal (via user PATH)
- **Desktop Entry**: `~/.local/share/applications/android-studio.desktop`
  - Appears in GNOME Application launcher / search / dock
  - Window class matched with `StartupWMClass=jetbrains-studio`
- **Application Icons**:
  - Vector: `~/.local/share/icons/hicolor/scalable/apps/android-studio.svg`
  - PNG: `~/.local/share/icons/hicolor/128x128/apps/android-studio.png`
- **Installer Archive**: `/home/hadi/Downloads/android-studio-rabbit1-linux.tar.gz` *(kept intact)*

---

## How to Launch

### 1. From Terminal
You can launch Android Studio or open the current project directly:

```bash
# Launch Android Studio
studio
# or
android-studio

# Open current directory as project
studio .
```

### 2. From Desktop (GUI)
- Open the application menu or press `Super` / `Windows key`.
- Search for **Android Studio** and click to open.
- You can right-click the dock icon and select **Pin to Dash / Add to Favorites**.

---

## Environment & Directories

- **Bundled JetBrains Runtime (JBR)**: Java 25.0.3 (`~/.local/android-studio/jbr/`)
- **Configuration Directory**: `~/.config/Google/AndroidStudio2026.2.1/`
- **Local Data Directory**: `~/.local/share/Google/AndroidStudio2026.2.1/`
- **Android SDK Path (Default)**: `~/Android/Sdk` (configured upon first launch wizard)
