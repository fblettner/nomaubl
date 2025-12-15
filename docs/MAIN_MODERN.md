# MainModern - Modernized NomaUBL Interface

## Overview

`MainModern.java` is a completely redesigned main interface for NomaUBL with modern UI/UX improvements using FlatLaf.

## What's New

### 🎨 **Modern Visual Design**
- **Clean Layout**: Better organized with clear sections
- **Tabbed Interface**: Separate tabs for different operations
- **Professional Styling**: Consistent with modern desktop applications
- **Rounded Corners**: Modern button and component styling
- **Better Spacing**: Improved padding and margins
- **Status Bar**: Real-time feedback with version info

### ⚡ **Enhanced Features**

#### Keyboard Shortcuts
- `Ctrl+,` (⌘+,) - Open Settings/Configuration Manager
- `Ctrl+Q` (⌘+Q) - Exit Application
- `Ctrl+G` (⌘+G) - Generate Report (when on Generate tab)
- `Ctrl+D` (⌘+D) - Download File (when on Get File tab)
- `Ctrl+O` (⌘+O) - Browse for Input File

#### Better User Experience
- **Tooltips**: Helpful hints on all controls
- **Input Validation**: Checks required fields before processing
- **Async Operations**: Background processing with progress feedback
- **Error Handling**: User-friendly error messages
- **Status Updates**: Real-time operation status

### 📋 **Two Main Tabs**

#### 1. Generate Report Tab
```
┌─────────────────────────────────────────┐
│ 📋 Template: [invoice          ▼]      │
│                          ⚙️ Settings ✕ Exit │
├─────────────────────────────────────────┤
│ 📄 Generate Report                      │
│                                         │
│ Input Configuration                     │
│ ┌───────────────────────────────────┐  │
│ │ Input File: [________________] 📁 │  │
│ │ Mode:       [SINGLE ▼]            │  │
│ └───────────────────────────────────┘  │
│                                         │
│           [🚀 Generate Report]          │
│                                         │
├─────────────────────────────────────────┤
│ Ready                    v2025.1 | ©... │
└─────────────────────────────────────────┘
```

**Features:**
- Input file browser with XML filter
- Template selection dropdown
- Single/Burst mode selector
- One-click report generation
- Background processing

#### 2. Get File from Server Tab
```
┌─────────────────────────────────────────┐
│ 📋 Template: [invoice          ▼]      │
│                          ⚙️ Settings ✕ Exit │
├─────────────────────────────────────────┤
│ 📥 Get File from Server                 │
│                                         │
│ Server File Information                 │
│ ┌───────────────────────────────────┐  │
│ │ Report:     [____________]        │  │
│ │ Version:    [____________]        │  │
│ │ Language:   [FR__________]        │  │
│ │ Job Number: [____________]        │  │
│ └───────────────────────────────────┘  │
│                                         │
│           [📥 Download File]            │
│                                         │
├─────────────────────────────────────────┤
│ Ready                    v2025.1 | ©... │
└─────────────────────────────────────────┘
```

**Features:**
- Server file download via SFTP
- Pre-filled default language (FR)
- Field validation
- Background download
- Connection status feedback

## Running MainModern

### Option 1: Using the Script (Recommended)
```bash
./test/run_modern_gui.sh
```

With custom config:
```bash
./test/run_modern_gui.sh /path/to/config.properties
```

### Option 2: Direct Java Command
```bash
java -cp "build:lib/*" Frames.MainModern
```

### Option 3: From Existing Script
The classic Main.java now uses FlatLaf styling but maintains the same layout.
To switch to the modern version, update your launch scripts to use `MainModern` instead of `Main`.

## Comparison: Classic vs Modern

| Feature | Classic Main | MainModern |
|---------|--------------|------------|
| **Layout** | NetBeans Form | Hand-coded modern layout |
| **Look & Feel** | FlatLaf (upgrade) | FlatLaf (native) |
| **Organization** | Mixed panel | Tabbed interface |
| **Status Bar** | ❌ None | ✅ Real-time feedback |
| **Tooltips** | ⚠️ Few | ✅ Comprehensive |
| **Keyboard Shortcuts** | ⚠️ Basic | ✅ Full support |
| **Input Validation** | ⚠️ Minimal | ✅ Complete |
| **Error Messages** | ⚠️ Technical | ✅ User-friendly |
| **Background Tasks** | ❌ Blocks UI | ✅ Async with feedback |
| **Settings Access** | ✅ Button | ✅ Modern config manager |
| **Visual Polish** | ⚠️ Dated | ✅ Modern |

## Key Improvements in Detail

### 1. Better Organization
**Before:** All controls mixed in panels
```
[Settings] [Exit]
[Template: ___]
[Input File: ___] [Browse]
[Report: ___] [Version: ___]
[Generate] [Get File]
```

**After:** Logical tab separation
```
Tab 1: Generate Report
  - Input Configuration section
  - Clear action button
  
Tab 2: Get File from Server
  - Server info section
  - Clear action button
```

### 2. Async Operations
**Before:** UI freezes during operations
```java
GenerateReport(...); // Blocks UI thread
```

**After:** Background processing
```java
SwingWorker<Void, Void> worker = new SwingWorker<>() {
    @Override
    protected Void doInBackground() {
        GenerateReport(...); // Runs in background
    }
    @Override
    protected void done() {
        // Update UI when complete
    }
};
worker.execute();
```

### 3. Input Validation
**Before:** No validation, errors occur during processing

**After:** Pre-flight checks
```java
if (txFileName.getText().isEmpty()) {
    JOptionPane.showMessageDialog(...);
    return;
}
```

### 4. Status Feedback
**Before:** No feedback until completion

**After:** Real-time status
```java
statusLabel.setText("Generating report...");
// ... process ...
statusLabel.setText("Report generated successfully");
```

## Integration with Existing Code

### Maintains Compatibility
- Same `MainInit(String pConfig)` method
- Same template loading logic
- Same configuration file format
- Same backend processing

### Coexistence
Both versions can coexist:
- `Main.java` - Classic NetBeans form (now with FlatLaf)
- `MainModern.java` - Modern hand-coded interface

Choose based on preference:
- Use `Main` for familiarity
- Use `MainModern` for better UX

## Configuration

### Initial Setup
```java
MainModern main = new MainModern();
main.MainInit("./test/config/config.properties");
main.setVisible(true);
```

### Template Loading
Automatically loads all templates from config file:
```java
// Loads templates and populates dropdown
// Excludes "global" from template list
MainInit(configPath);
```

## Technical Details

### Architecture
```
MainModern
├── UI Layer (Swing components)
├── Event Handlers (button actions)
├── Business Logic (report generation)
├── Background Tasks (SwingWorker)
└── Integration (existing backend)
```

### Key Components
- **JTabbedPane**: Organizes different operations
- **SwingWorker**: Handles async operations
- **GridBagLayout**: Flexible form layouts
- **TitledBorder**: Clear section separation
- **Status Bar**: Real-time feedback

### Memory Efficiency
- Components created on-demand
- Proper resource cleanup
- No memory leaks from workers

## Troubleshooting

### FlatLaf Not Loading?
```bash
# Check if FlatLaf JAR exists
ls -la lib/flatlaf*.jar

# If missing, re-download
cd lib
curl -L -O https://repo1.maven.org/maven2/com/formdev/flatlaf/3.5.2/flatlaf-3.5.2.jar
```

### UI Components Not Showing?
Rebuild the project:
```bash
./test/build_modern.sh
```

### Settings Button Not Working?
Ensure ManageConfigModern.class exists:
```bash
ls -la build/Frames/ManageConfigModern.class
```

## Migration Guide

### From Classic Main to MainModern

**Step 1:** Update launch script
```bash
# Old
java -cp "build:lib/*" Frames.Main

# New
java -cp "build:lib/*" Frames.MainModern
```

**Step 2:** No code changes needed!
Same initialization:
```java
main.MainInit(configPath);
```

**Step 3:** Test both versions side-by-side
```bash
# Classic (with FlatLaf styling)
java -cp "build:lib/*" Frames.Main

# Modern (new UI)
java -cp "build:lib/*" Frames.MainModern
```

## Future Enhancements

Potential improvements:
- [ ] Recent files menu
- [ ] Drag-and-drop file input
- [ ] Progress bars for long operations
- [ ] Dark theme toggle
- [ ] Template preview
- [ ] Batch processing
- [ ] Export configuration
- [ ] Advanced settings panel
- [ ] Help system (F1)
- [ ] Multi-language UI

## Credits

- **Original**: NetBeans-generated Main.java (2018)
- **Modernized**: Hand-coded MainModern.java (2025)
- **UI Framework**: FlatLaf 3.5.2
- **License**: NOMANA-IT © 2018-2025

---

**Enjoy the modern NomaUBL interface! 🚀**
