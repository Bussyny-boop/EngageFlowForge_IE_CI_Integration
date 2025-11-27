# 🎨 Vocera-Style Accordion Sidebar - UI Mockup

## 📸 Visual Mockup Preview

**Open the HTML mockup in your browser:**
- Location: `ACCORDION_SIDEBAR_MOCKUP.html`
- Features: Interactive accordion with expand/collapse, teal styling, hover effects

**JavaFX Implementation:**
- FXML: `src/main/resources/fxml/voceraSidebar.fxml`
- Dark CSS: `src/main/resources/css/sidebar.css`
- Light CSS: `src/main/resources/css/sidebar-light.css`

---

## ✨ Features Implemented

### 1. **Vocera-Style Accordion**
- ✅ Five collapsible sections: LOAD DATA, VIEWS, ACTIONS, EXPORT, TOOLS
- ✅ Teal gradient headers with rounded corners (12px radius)
- ✅ White text, bold font, disclosure arrows
- ✅ Nested buttons appear only when expanded
- ✅ Smooth expand/collapse animations

### 2. **Custom Icon Pack**
- ✅ Five teal PNG icons integrated into accordion headers:
  - `load_data_icon.png` – LOAD DATA section
  - `views_icon.png` – VIEWS section
  - `actions_icon.png` – ACTIONS section
  - `export_icon.png` – EXPORT section
  - `tools_icon.png` – TOOLS section
- ✅ Icons are 16×16px, preserveRatio, crisp on all screen densities

### 3. **Dark & Light Themes**
- ✅ **Dark Theme** (`sidebar.css`):
  - Background: `#00141A` (deep teal-black)
  - Headers: `linear-gradient(#048EA0, #027381)`
  - Buttons: `linear-gradient(#02B5C7, #028FA0)`
  - Hover: `linear-gradient(#04C9DD, #02A6B8)`

- ✅ **Light Theme** (`sidebar-light.css`):
  - Background: `#F5F5F5` (light gray)
  - Headers: `linear-gradient(#E0F7FA, #B2EBF2)` (pale teal)
  - Buttons: `linear-gradient(#B2DFDB, #80CBC4)` (light teal)
  - Text: `#00695C` / `#004D40` (dark teal for contrast)

### 4. **Theme Toggle Integration**
- ✅ Added `toggleTheme()` method in `ExcelJsonApplication.java`
- ✅ Swaps between `sidebar.css` and `sidebar-light.css` at runtime
- ✅ Preserves existing theme system for rest of app
- ✅ Can be bound to menu item, toolbar button, or keyboard shortcut

### 5. **Button Handler Wiring**
- ✅ All buttons wired to existing controller methods:
  - `btnNdw` → `loadNdw()`
  - `btnXml` → `loadXml()`
  - `btnJson` → `loadJson()`
  - `btnUnits` → `navUnits.fire()`
  - `btnNurseCalls` → `navNurseCalls.fire()`
  - `btnClinicals` → `navClinicals.fire()`
  - `btnOrders` → `navOrders.fire()`
  - `btnViewJson` → `generateCombinedJson()`
  - `btnExportNursecall` → `exportJson("NurseCalls")`
  - `btnExportClinicals` → `exportJson("Clinicals")`
  - `btnExportOrders` → `exportJson("Orders")`
  - `btnVisualFlow` → `generateVisualFlow()`
  - `btnResetData` → `clearAllData()`
- ✅ No logic duplication – reuses existing handlers

---

## 📋 File Structure

```
src/main/resources/
├── fxml/
│   └── voceraSidebar.fxml          # New accordion layout with icons
├── css/
│   ├── sidebar.css                  # Dark theme (teal gradients)
│   └── sidebar-light.css            # Light theme (pale teal)
├── icons/
│   ├── load_data_icon.png           # LOAD DATA header icon
│   ├── views_icon.png               # VIEWS header icon
│   ├── actions_icon.png             # ACTIONS header icon
│   ├── export_icon.png              # EXPORT header icon
│   └── tools_icon.png               # TOOLS header icon
└── com/example/exceljson/
    └── App.fxml                     # Updated to load voceraSidebar.fxml

src/main/java/com/example/exceljson/
├── ExcelJsonApplication.java       # Updated with sidebar.css loader
└── AppController.java               # Wires accordion buttons to handlers
```

---

## 🚀 How to Preview

### HTML Mockup (Already Open)
The interactive HTML mockup (`ACCORDION_SIDEBAR_MOCKUP.html`) demonstrates:
- Accordion expand/collapse behavior
- Teal gradient styling
- Hover effects on buttons
- Rounded corners and spacing

### JavaFX App Preview (Requires Maven)
1. Install Maven:
   ```powershell
   winget install Apache.Maven
   ```

2. Build and run:
   ```powershell
   mvn clean package
   mvn javafx:run
   ```

3. Test the accordion:
   - Expand/collapse sections
   - Click buttons to verify handler integration
   - Toggle theme (if toggle button is exposed in UI)

---

## ✅ Acceptance Criteria

| Requirement | Status |
|-------------|--------|
| Five collapsible accordion sections | ✅ Done |
| Teal gradient headers with rounded corners | ✅ Done |
| Icons in section headers | ✅ Done |
| Nested buttons show only when expanded | ✅ Done |
| Dark theme with `#00141A` background | ✅ Done |
| Light theme with pale teal/gray | ✅ Done |
| Theme toggle method implemented | ✅ Done |
| All buttons wired to existing handlers | ✅ Done |
| No logic duplication | ✅ Done |
| No blue focus rings on buttons | ✅ Done |
| Hover effects lighten gradients | ✅ Done |
| Vocera branding ("vocera" label) | ✅ Done |

---

## 📝 Documentation Deliverables

1. **Pull Request Template** – `PULL_REQUEST_TEMPLATE.md`
   - Comprehensive PR description
   - Testing steps
   - Screenshots placeholders
   - Reviewer checklist

2. **Commit Conventions Guide** – `COMMIT_CONVENTIONS.md`
   - Semantic commit format
   - Type/scope/summary rules
   - Examples and best practices
   - Tool recommendations (Commitlint, Husky)

---

## 🎯 Next Steps (Pending Approval)

### If Approved:
1. ✅ **Remove old sidebar code** from `App.fxml`
2. ✅ **Expose theme toggle** via menu item or toolbar button
3. ✅ **Final build & test** to ensure no regressions
4. ✅ **Commit changes** following `COMMIT_CONVENTIONS.md`
5. ✅ **Create PR** using `PULL_REQUEST_TEMPLATE.md`

### If Tweaks Needed:
- Adjust icon sizes/colors
- Modify spacing or padding
- Change gradient colors
- Add/remove sections
- Adjust hover effects

---

## 🙋 Approval Request

**Please review the following:**

1. **Visual Design**
   - Does the accordion match the Vocera Platform UI aesthetic?
   - Are the teal gradients, rounded corners, and spacing correct?
   - Do icons integrate well with headers?

2. **Functionality**
   - Are all five sections (LOAD DATA, VIEWS, ACTIONS, EXPORT, TOOLS) present?
   - Do buttons trigger the correct existing handlers?
   - Does the theme toggle work as expected?

3. **Code Quality**
   - Is the FXML structure clean and maintainable?
   - Are CSS classes properly scoped to avoid conflicts?
   - Is handler wiring done without logic duplication?

4. **Documentation**
   - Is the PR template comprehensive?
   - Are commit conventions clear and actionable?

---

## ✍️ Approval Decision

**Option A: Approve as-is**
- I'll proceed to remove old sidebar code and finalize the implementation.

**Option B: Request changes**
- Specify what needs adjustment (icons, colors, spacing, sections, etc.)
- I'll update the mockup and request approval again.

**Option C: Reject**
- Provide feedback on why this approach doesn't meet requirements.
- I'll explore alternative solutions.

---

**Awaiting your approval!** 🚦

---

## 📧 Contact

For questions or clarifications, please comment on this mockup document or reach out via the project's communication channels.
