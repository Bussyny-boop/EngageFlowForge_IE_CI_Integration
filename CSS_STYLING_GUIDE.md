# CSS Styling Guide - Engage FlowForge 2.0

## Color Palette

### Primary Colors
```css
Teal Primary:   #00979D  /* Main brand color */
Teal Dark:      #008387  /* Gradients and hover states */
Teal Light:     #00A8AF  /* Active states */
Teal Accent:    #4ECDC4  /* Preview buttons */
```

### Neutral Colors
```css
Background:     #f7f8fa  /* Main app background */
White:          #ffffff  /* Cards and inputs */
Light Gray:     #f0f3f5  /* Filter bars */
Border Gray:    #e0e4e8  /* Card borders */
Line Gray:      #cdd4db  /* Input borders */
Dark Gray:      #333333  /* Text */
Medium Gray:    #666666  /* Secondary text */
```

### Functional Colors
```css
Success:        #27ae60  /* Not yet used */
Warning:        #E74C3C  /* Clear All button */
Error:          #e74c3c  /* Alerts */
```

## Typography System

### Font Stack
```css
Primary: "Segoe UI", "Open Sans", "Roboto", sans-serif
Monospace: "Consolas", "Monaco", "Courier New", "Liberation Mono", monospace
```

### Font Sizes
```css
.root              { font-size: 13px }      /* Base size */
.section-header    { font-size: 14px }      /* Section titles */
.header-bar Label  { font-size: 20px }      /* App title */
.button            { font-size: 13px }      /* Buttons */
.table-cell        { font-size: 12px }      /* Table content */
.status-label      { font-size: 12px }      /* Status bar */
.tooltip           { font-size: 12px }      /* Tooltips */
.adapter-field     { font-size: 12px }      /* Small inputs */
```

### Font Weights
```css
Normal:   400 (default)
Medium:   500 (status label)
Semibold: 600 (buttons, tab labels)
Bold:     700 (section headers)
```

## Spacing System

### Padding Values
```css
Controls Container:  15px 20px 12px 20px  /* T R B L */
Control Section:     12px 15px            /* Vertical Horizontal */
Header Bar:          12px 20px
Filter Bar:          8px 10px
Buttons:             8px 16px
Text Fields:         6px 8px
JSON Preview:        10px
```

### Margins & Spacing
```css
VBox/HBox spacing:   12px  /* Standard spacing */
Section spacing:     8px   /* Within sections */
Large gaps:          15px  /* Between major elements */
Tight spacing:       3px   /* Labels and inputs */
```

## Border & Radius System

### Border Radius
```css
Large (cards):       6px
Medium (inputs):     4px
Small (checkboxes):  3px
```

### Border Widths
```css
Default:             1px
Thick (sections):    1.5px
Focus:               2px
```

## Shadow Effects

### Drop Shadows
```css
/* Header bar */
dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3)

/* Controls container */
dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2)

/* Control sections */
dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1)

/* Buttons */
dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1)

/* Tabs */
dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2)

/* Selected tabs */
dropshadow(gaussian, rgba(0,151,157,0.3), 5, 0, 0, 2)

/* Focus states */
dropshadow(gaussian, rgba(0,151,157,0.25), 4, 0, 0, 0)
```

## Component Styles

### 1. Buttons

#### Primary Button
```css
.button {
    -fx-background-color: linear-gradient(to bottom, #00979D, #008387);
    -fx-text-fill: white;
    -fx-padding: 8 16;
    -fx-background-radius: 5;
    -fx-font-weight: 600;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);
}

.button:hover {
    -fx-background-color: linear-gradient(to bottom, #00A8AF, #00979D);
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 0, 2);
    -fx-scale-x: 1.02;
    -fx-scale-y: 1.02;
}
```

#### Preview Button
```css
.button-preview {
    -fx-background-color: linear-gradient(to bottom, #4ECDC4, #44B3AC);
    -fx-text-fill: white;
}

.button-preview:hover {
    -fx-background-color: linear-gradient(to bottom, #5ED4CB, #4ECDC4);
}
```

#### Secondary Button
```css
.button-secondary {
    -fx-background-color: linear-gradient(to bottom, #E8EAED, #D5D8DC);
    -fx-text-fill: #333;
}
```

#### Warning Button
```css
.button-warning {
    -fx-background-color: linear-gradient(to bottom, #E74C3C, #C0392B);
    -fx-text-fill: white;
}
```

### 2. Control Sections
```css
.control-section {
    -fx-background-color: #ffffff;
    -fx-border-color: #e0e4e8;
    -fx-border-width: 1;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-padding: 12 15;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);
}
```

### 3. Section Headers
```css
.section-header {
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-text-fill: #00979D;
    -fx-padding: 0 0 4 0;
}
```

### 4. Tables
```css
/* Table container */
.table-view, .data-table {
    -fx-border-color: #CDD4DB;
    -fx-background-color: white;
    -fx-border-radius: 4;
    -fx-border-width: 1;
}

/* Column headers */
.table-view .column-header-background {
    -fx-background-color: linear-gradient(to bottom, #F5F7F9, #EBEEF1);
}

.table-view .column-header {
    -fx-text-fill: #333;
    -fx-font-weight: bold;
    -fx-font-size: 12px;
}

/* Table rows */
.table-row-cell {
    -fx-background-color: white;
}

.table-row-cell:odd {
    -fx-background-color: #FAFBFC;
}

.table-row-cell:hover {
    -fx-background-color: #E8F5F6;
}

.table-row-cell:selected {
    -fx-background-color: #B3E5E6;
    -fx-text-fill: #333;
}
```

### 5. Tabs
```css
.tab-pane {
    -fx-border-color: #d5dce3;
    -fx-border-width: 1;
    -fx-border-radius: 6;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);
}

.tab {
    -fx-background-color: #F5F7F9;
    -fx-background-radius: 6 6 0 0;
    -fx-border-color: #CDD4DB;
    -fx-padding: 10 18 10 18;
}

.tab:selected {
    -fx-background-color: linear-gradient(to bottom, #00979D, #008387);
    -fx-effect: dropshadow(gaussian, rgba(0,151,157,0.3), 5, 0, 0, 2);
}

.tab:selected .tab-label {
    -fx-text-fill: white;
    -fx-font-weight: bold;
}
```

### 6. Filter Bar
```css
.filter-bar {
    -fx-background-color: #f0f3f5;
    -fx-border-color: #d5dce3;
    -fx-border-width: 0 0 2 0;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);
}

.filter-combo {
    -fx-background-color: white;
    -fx-border-color: #00979D;
    -fx-border-width: 1.5;
    -fx-border-radius: 4;
    -fx-effect: dropshadow(gaussian, rgba(0,151,157,0.2), 3, 0, 0, 1);
}
```

### 7. Text Fields
```css
.text-field {
    -fx-border-color: #CDD4DB;
    -fx-background-color: white;
    -fx-border-width: 1.5;
    -fx-border-radius: 4;
    -fx-padding: 6 8;
}

.text-field:focused {
    -fx-border-color: #00979D;
    -fx-border-width: 2;
    -fx-effect: dropshadow(gaussian, rgba(0,151,157,0.25), 4, 0, 0, 0);
}
```

### 8. JSON Preview
```css
.json-preview-container {
    -fx-padding: 10;
    -fx-background-color: white;
    -fx-border-color: #e0e4e8;
    -fx-border-width: 1 0 0 0;
}

.json-preview-area {
    -fx-font-family: "Consolas", "Monaco", "Courier New", monospace;
    -fx-font-size: 12px;
    -fx-border-color: #CDD4DB;
}

.json-preview-area .content {
    -fx-background-color: #FAFBFC;
}
```

### 9. Checkboxes
```css
.check-box .box {
    -fx-background-color: white;
    -fx-border-color: #CDD4DB;
    -fx-border-width: 1.5;
    -fx-border-radius: 3;
}

.check-box:selected .box {
    -fx-background-color: #00979D;
    -fx-border-color: #007C80;
}

.check-box:selected .mark {
    -fx-background-color: white;
}
```

### 10. Scrollbars
```css
.scroll-bar {
    -fx-background-color: #F5F7F9;
}

.scroll-bar .thumb {
    -fx-background-color: #C5CDD5;
    -fx-background-radius: 4;
}

.scroll-bar .thumb:hover {
    -fx-background-color: #00979D;
}
```

## State Management

### Interactive States
```css
/* Default */
Normal state

/* Hover */
:hover {
    Lighter gradient
    Increased shadow
    Optional scale (buttons: 1.02)
}

/* Pressed */
:pressed {
    Darker solid color
    Reduced shadow
    Optional scale (buttons: 0.98)
}

/* Focused */
:focused {
    Teal border (2px)
    Colored shadow glow
}

/* Disabled */
:disabled {
    Opacity: 0.5
    Gray background
    No effects
}
```

## Animation Effects

### Scale Transitions (Buttons)
```css
/* Hover */
-fx-scale-x: 1.02;
-fx-scale-y: 1.02;

/* Press */
-fx-scale-x: 0.98;
-fx-scale-y: 0.98;
```

### Shadow Transitions
```css
/* Normal → Hover */
Shadow blur: 3px → 5px
Shadow opacity: 0.1 → 0.15

/* Pressed */
Shadow blur: 2px
Shadow opacity: 0.12
```

### Tab Fade (Java)
```java
FadeTransition(Duration.millis(200))
fromValue: 0.0
toValue: 1.0
```

## Accessibility Considerations

### Color Contrast
- Text on teal: White (#FFFFFF) - WCAG AA compliant
- Text on white: Dark gray (#333333) - WCAG AAA compliant
- Secondary text: Medium gray (#666666) - WCAG AA compliant

### Focus Indicators
- All interactive elements have visible focus states
- 2px teal border with shadow glow
- High contrast for keyboard navigation

### Font Sizes
- Minimum 12px for body text
- 13px for interactive elements
- Clear hierarchy for scanning

## Usage Examples

### Creating a New Section
```xml
<VBox spacing="8" styleClass="control-section">
    <Label text="My Section" styleClass="section-header" />
    <!-- Section content -->
</VBox>
```

### Adding a Primary Button
```xml
<Button text="Action" styleClass="button">
    <tooltip>
        <Tooltip text="Description" />
    </tooltip>
</Button>
```

### Creating a Filter Bar
```xml
<HBox styleClass="filter-bar" spacing="10">
    <Label text="🔍 Filter:" style="-fx-font-weight: bold;" />
    <ComboBox styleClass="filter-combo" />
</HBox>
```

## CSS Class Reference

| Class Name | Purpose |
|------------|---------|
| `.border-pane` | Main container |
| `.header-bar` | Top header section |
| `.controls-container` | All controls wrapper |
| `.control-section` | Individual section card |
| `.section-header` | Section title |
| `.status-label` | Bottom status bar |
| `.button` | Primary button |
| `.button-preview` | Preview button variant |
| `.button-secondary` | Secondary button |
| `.button-warning` | Warning/destructive button |
| `.adapter-field` | Adapter input fields |
| `.filter-bar` | Filter section background |
| `.filter-combo` | Enhanced combo box |
| `.tab-content` | Tab content wrapper |
| `.data-table` | Enhanced table |
| `.json-preview-container` | JSON section wrapper |
| `.json-preview-area` | JSON text area |

## Maintenance Notes

1. **Consistency**: Always use the defined color variables
2. **Gradients**: Use `to bottom` direction for all gradients
3. **Shadows**: Follow the blur/opacity pattern for depth
4. **Radius**: Use 6px for cards, 4px for inputs/tabs, 3px for small elements
5. **Spacing**: Stick to 8px, 10px, 12px, 15px, 20px increments
6. **Hover Effects**: Always include shadow enhancement on hover
7. **Focus States**: Always add 2px teal border + shadow glow
