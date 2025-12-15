# JAVA package with Oracle BI Publisher API

Go to [https://docs.nomana-it.fr/bipublisher/bip-api/nomabip](https://docs.nomana-it.fr/api/jde-api/jdebip/) for documentation

### Built with VSCode and JDK 17

## 🎨 Modern UI (NEW!)

NomaUBL now features **five** completely modernized interfaces with **FlatLaf** for a professional, contemporary look!

### ✨ Modern Interfaces

1. **ManageConfigModern** - Configuration Manager
   - Live search and filtering
   - Keyboard shortcuts (Ctrl+O, Ctrl+N, Ctrl+D, F5)
   - Auto-opens modern editors

2. **MainModern** - Main Report Generator
   - Tabbed interface (Generate | Get File)
   - Async operations (no UI freezing)
   - Input validation

3. **ManageGlobalModern** - Global Settings Editor
   - Organized 4-tab layout (Directories | Processing | Database | Server)
   - Change tracking with unsaved indicator
   - Field tooltips and validation

4. **ManageTemplateModern** - Template Configuration Editor
   - Organized 3-tab layout (Document | Processing | Advanced)
   - XPath configuration for UBL documents
   - Processing and routing configuration
   - Helpful configuration tips

5. **GetXMLModern** - File Download Interface
   - Clean input form with validation
   - Async SFTP download (no UI blocking)
   - Progress indicator and status feedback
   - Keyboard shortcuts (Ctrl+D, Enter, Esc)

**Features:**
- 🌙 **VS Code Dark Theme** - Beautiful dark interface inspired by VS Code
- ✨ Modern flat design with clean aesthetics
- 🔍 Live search and filtering
- ⌨️ Full keyboard shortcuts (Ctrl+S, Ctrl+D, Esc, Enter)
- 📊 Real-time status feedback
- 💡 Helpful tooltips everywhere
- 🎯 Better organization with tabs
- ⚡ Async operations (no UI freezing)
- 💾 Change tracking and warnings

See [docs/MODERNIZATION.md](docs/MODERNIZATION.md), [docs/MAIN_MODERN.md](docs/MAIN_MODERN.md), and [docs/GETXML_MODERN.md](docs/GETXML_MODERN.md) for details.

## Functionalities
This api was initially developed to integrate easily JDEdwards and BI Publisher regardless of JDEdwards Tools Release.\
Nothing to install, only to use. Samples scripts are provided into the test directory. It can now be used for any spool with or without JD Edwards (BI Publisher license is needed).
- Generate PDF and XML files simultaneously from a xml spool
- Burst or single mode
- Number of CPU for parallel processing and improving performance
- Add Ghostscript for PDF compatibility
- Document indexation and errors into an Oracle Database (optional)
- Integrate Java class NOMABC to print Barcode 128 (see github repository)

## Command Line Usage
```bash
jar cfm nomaubl.jar ./src/nomaubl/manifest.txt -C ./build .
cp ./test/input/3911372_RI_00001.xml ./test/input/invoice/.
java -jar nomaubl.jar -run ./test/config/config.properties invoice 3911372_RI_00001 BURST 1
java -jar nomaubl.jar -config ./test/config/config.properties
```