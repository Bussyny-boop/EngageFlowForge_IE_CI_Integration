# New App Icon - Design Documentation

## Icon Overview

A new application icon has been generated featuring a bold letter "V" with Teal and Light Orange colors.

## Color Palette

- **Primary Color (Teal)**: `#008080` - Used for the circular background
- **Accent Color (Light Orange)**: `#FFB347` - Used for highlights and border accent
- **Letter Color (White)**: `#FFFFFF` - Used for the bold "V" letter

## Design Elements

### 1. Shape
- Circular background with teal color
- Light orange accent in the top-right area creating a subtle gradient effect
- Light orange border outline for depth

### 2. Typography
- Bold, thick "V" letter in white
- Centered in the circle
- Light orange highlight on the right leg of the V for visual interest
- Stroke width scales proportionally with icon size

### 3. Size Variations

Generated icons for different display contexts:
- `icon.png` - 256x256px (main high-resolution icon)
- `icon.ico` - Multi-size ICO file (16, 32, 48, 256px) for Windows
- `icon_16.png` - 16x16px (taskbar/small display)
- `icon_32.png` - 32x32px (standard display)
- `icon_48.png` - 48x48px (medium display)
- `icon_64.png` - 64x64px (large display)
- `icon_128.png` - 128x128px (very large display)

## Visual Representation

```
    ╭─────────────────╮
   ╱   🟠 (Orange)    ╲
  │    ╱╲             │
 │    ╱  ╲            │
 │   ╱    ╲           │  ← Teal (#008080) background
 │  ╱  V   ╲          │
 │ ╱  (white) ╲       │
 │╱            ╲      │
  ╲             ╱
   ╲           ╱
    ╰─────────╯
```

## ASCII Art Preview (Simplified)

```
        ▓▓▓▓▓▓▓▓▓
     ▓▓▓▓▓▓▓▓▓▓▓▓▓▓
   ▓▓▓▓░░▓▓▓▓▓▓▓▓▓▓▓
  ▓▓▓░░░░▓▓▓▓▓▓▓▓▓▓▓▓
 ▓▓▓▓░░░░▓▓▓▓▓▓▓▓▓▓▓▓▓
 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓▓▓██▓▓▓▓▓▓▓▓▓▓▓██▓▓▓▓
▓▓▓▓██▓▓▓▓▓▓▓▓██▓▓▓▓▓▓
▓▓▓▓▓██▓▓▓▓▓██░▓▓▓▓▓▓▓
 ▓▓▓▓▓██▓▓██░░▓▓▓▓▓▓▓
 ▓▓▓▓▓▓███░░░▓▓▓▓▓▓▓▓
  ▓▓▓▓▓▓█░░░▓▓▓▓▓▓▓▓
   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
     ▓▓▓▓▓▓▓▓▓▓▓
        ▓▓▓▓▓▓

Legend:
▓ = Teal (#008080)
░ = Light Orange (#FFB347) 
█ = White V letter
```

## Usage in Application

### JavaFX Application
The icon is loaded in `ExcelJsonApplication.java`:
- Path: `/icon.png`
- Displays in: Window title bar, Windows taskbar, Alt+Tab switcher

### Windows Installer
The ICO file is used for:
- Windows installer package
- Desktop shortcuts
- Start menu entries

## Implementation Details

### Icon Loading Code
```java
String iconPath = "/icon.png";
try (InputStream iconInputStream = getClass().getResourceAsStream(iconPath)) {
    if (iconInputStream != null) {
        primaryStage.getIcons().add(new Image(iconInputStream));
    }
}
```

### File Locations
All icon files are located in:
```
src/main/resources/
├── icon.png      (256x256 - main)
├── icon.ico      (multi-size)
├── icon_16.png
├── icon_32.png
├── icon_48.png
├── icon_64.png
└── icon_128.png
```

## Design Rationale

1. **Bold V Letter**: Represents "Vocera" and makes the icon instantly recognizable
2. **Teal Color**: Professional, healthcare-associated color that stands out
3. **Light Orange Accent**: Adds warmth and visual interest without overwhelming
4. **Circular Shape**: Modern, friendly, and works well at small sizes
5. **High Contrast**: White letter on teal ensures readability at all sizes

## Platform Compatibility

- ✅ Windows 10/11 - Taskbar and window chrome
- ✅ macOS - Dock and window title (JavaFX support)
- ✅ Linux - Window managers and panels
- ✅ Scalable - Looks crisp from 16px to 256px

## Files Generated

| File | Size | Purpose |
|------|------|---------|
| icon.png | 256x256 | Primary application icon, high resolution |
| icon.ico | Multi | Windows installer and shortcuts |
| icon_16.png | 16x16 | Taskbar/small display |
| icon_32.png | 32x32 | Standard display |
| icon_48.png | 48x48 | Medium display |
| icon_64.png | 64x64 | Large display |
| icon_128.png | 128x128 | Very large display |
