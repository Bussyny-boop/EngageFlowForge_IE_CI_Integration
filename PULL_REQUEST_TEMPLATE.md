# Pull Request: Add Vocera-Style Accordion Sidebar with Icons and Theme Toggle

## 📋 Overview

This PR refactors the application's left navigation panel to match the **Vocera Platform Server UI style** and introduces a **dark/light theme toggle** along with a custom **icon pack**. The new sidebar features collapsible accordion sections with teal gradients, rounded corners, and professional icons.

---

## 🎨 What's Changed

### 🧩 New UI Components

1. **`voceraSidebar.fxml`** – New FXML file defining a collapsible `Accordion` with five sections:
   - **LOAD DATA** – NDW File, Engage XML, Engage JSON
   - **VIEWS** – Units, Nurse Calls, Clinicals, Orders
   - **ACTIONS** – View JSON
   - **EXPORT** – Nursecall, Clinicals, Orders
   - **TOOLS** – Visual Flow, Reset Data

2. **`sidebar.css`** – Dark/teal theme matching the Vocera web console. Styles headers, nested buttons, and hover states using gradients and rounded corners.

3. **`sidebar-light.css`** – Light variant of the sidebar styles. Pale backgrounds with darker teal text provide high contrast for accessibility.

4. **Icon Pack** – Five custom teal icons added to `src/main/resources/icons/`:
   - `load_data_icon.png`
   - `views_icon.png`
   - `actions_icon.png`
   - `export_icon.png`
   - `tools_icon.png`
   
   Icons are loaded via `<ImageView>` in each `<TitledPane>` header.

---

## 🚦 Theme Toggle

A new **theme toggle** has been implemented in `ExcelJsonApplication.java`. It switches between dark and light modes by swapping the loaded stylesheet at runtime:

```java
private boolean darkMode = true;

private void toggleTheme() {
    darkMode = !darkMode;
    Scene scene = primaryStage.getScene();
    scene.getStylesheets().clear();
    
    // Load base theme
    String themePath = darkMode ? "/css/dark-theme.css" : "/css/vocera-theme.css";
    var themeResource = getClass().getResource(themePath);
    if (themeResource != null) {
        scene.getStylesheets().add(themeResource.toExternalForm());
    }
    
    // Load sidebar theme
    String sidebarPath = darkMode ? "/css/sidebar.css" : "/css/sidebar-light.css";
    var sidebarResource = getClass().getResource(sidebarPath);
    if (sidebarResource != null) {
        scene.getStylesheets().add(sidebarResource.toExternalForm());
    }
}
```

**Expose via:**
- Menu item: `View → Toggle Theme`
- Toolbar button with icon
- Keyboard shortcut: `Ctrl+T`

The toggle state can persist across restarts using `Preferences` if desired.

---

## 🧹 Layout Integration

- **Old sidebar removed:** Legacy sidebar code has been removed from `App.fxml`.
- **New sidebar loaded:** The main controller now loads `voceraSidebar.fxml` into the `sidebarContainer` during initialization.
- **Handler preservation:** All existing button handlers remain intact. Each `<Button>` in `voceraSidebar.fxml` binds to existing controller methods via lookup (e.g., `btnNdw` → `loadNdw()`).
- **Icons integrated:** `<ImageView>` components load icons from `/icons/` for each accordion section header.

---

## ✅ How to Test

1. **Run the application:**
   ```bash
   mvn clean package
   mvn javafx:run
   ```

2. **Verify accordion behavior:**
   - The left panel should display a Vocera-style accordion with teal headers and icons.
   - Expand/collapse each section to reveal nested buttons.
   - Verify that clicking buttons triggers their original functionality (e.g., Load NDW, Export JSON).

3. **Test theme toggle:**
   - Switch between dark and light themes using the toggle control.
   - Confirm that:
     - Backgrounds, header gradients, and text colors switch correctly.
     - Icons remain visible and crisp on both backgrounds.
     - No layout shifts or visual glitches occur.

4. **Check icons:**
   - Ensure custom icons display properly in accordion headers.
   - Verify icons are teal-colored and match the Vocera aesthetic.

---

## 📝 Additional Notes

- **CSS scoping:** All styles are scoped to sidebar-specific classes (`.sidebar-*`) to avoid conflicts with other UI components.
- **Future extensibility:** To add or modify sidebar sections, update `voceraSidebar.fxml` and adjust the corresponding CSS classes.
- **Accessibility:** Light theme provides high contrast for better readability in bright environments.
- **Performance:** No performance impact observed; accordion expand/collapse is smooth on both themes.

---

## 🔗 Related Issues

Closes #XXX (replace with actual issue number)

---

## 📸 Screenshots

### Dark Theme (Before & After)
| Before | After |
|--------|-------|
| ![Old Sidebar](link-to-before.png) | ![New Vocera Accordion](link-to-after.png) |

### Light Theme
![Light Theme Accordion](link-to-light-theme.png)

---

## 👥 Reviewers

Please review:
- UI/UX consistency with Vocera Platform
- Theme toggle functionality
- Icon integration
- Code quality and JavaFX best practices

---

**Ready for review!** 🚀
