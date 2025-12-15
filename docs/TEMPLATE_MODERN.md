# ManageTemplateModern - Template Configuration Editor

A modern interface for configuring UBL document templates with organized tabs and helpful guidance.

## Overview

ManageTemplateModern provides a clean, intuitive interface for managing template configurations. Each template defines how to process and transform specific document types (invoices, credit notes, etc.).

## Interface Layout

### Header Section
- **Template Name**: Unique identifier for the template
- **Description**: Brief description of what this template processes

### Tab 1: 📄 Document

Configuration for document identification and business data extraction.

#### Document Identification
- **Document ID**: XPath expression to locate the main document identifier
- **Activity**: Business activity code
- **Document Type**: Type of document (invoice, order, credit note, etc.)

#### JD Edwards Integration
- **JDE Type**: JD Edwards document type mapping
- **Company**: JDE company code

#### Document Data (XPath)
- **Customer Number**: XPath to extract customer identifier
- **Amount**: XPath to extract document total amount
- **Document Date**: XPath to extract document date
- **Due Date**: XPath to extract payment due date

**XPath Examples:**
```xpath
/Invoice/ID                          # Simple path
//Customer/@Number                   # Attribute
/Invoice/LineItems/Line[1]/Amount    # Indexed element
```

### Tab 2: 🔧 Processing

Configuration for document transformation and routing.

#### XSL Transformation
- **Transform**: Enable/disable transformation (Y/N)
- **Transform XSL**: Path to pre-transformation stylesheet
- **Main XSL**: Path to main BI Publisher template
- **RTF Template**: Path to RTF template file

**Path Variables:**
- `%APP_HOME%` - Application root directory
- `%PROCESS_HOME%` - Processing directory
- `%TEMPLATE%` - Current template name

#### Bursting Configuration
- **Burst Key**: XPath expression for document splitting in burst mode
  - Example: `//Customer/@ID` to split by customer

#### Routing
- **Routing Code**: Code for document routing/distribution

### Tab 3: ⚙️ Advanced

Performance settings and configuration guidance.

#### Performance Settings
- **Number of CPUs**: Processor count for parallel processing
  - `1` = Single processor (default, sequential)
  - `2+` = Parallel processing (burst mode only)
  - Higher = Faster (if cores available)

#### Configuration Tips
Built-in help panel with:
- XPath syntax examples
- File path variable reference
- CPU configuration guidance

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| **Ctrl+S** | Save template configuration |
| **Esc** | Cancel and close |

## Features

### Change Tracking
- Asterisk (*) in title bar when modified
- Unsaved changes warning on close
- Status bar shows modification state

### Validation
- Template name required
- Duplicate name detection
- Field-level tooltips with guidance

### User Experience
- Organized tabs reduce clutter
- Scrollable panels for all content
- Helpful tips integrated into UI
- Professional FlatLaf styling

## Usage Examples

### Creating a New Invoice Template

1. **Open Config Manager** → Add Template
2. **Enter Name**: `invoice_ubl`
3. **Document Tab**:
   ```
   Document ID: /Invoice/ID
   Activity: INVOICE
   Document Type: Invoice
   JDE Type: P42565
   ```

4. **Processing Tab**:
   ```
   Transform: Y
   Transform XSL: %APP_HOME%/template/transform.xsl
   Main XSL: %APP_HOME%/template/invoice.xsl
   Burst Key: //Customer/@Number
   ```

5. **Advanced Tab**:
   ```
   Number of CPUs: 4
   ```

6. **Save**: Ctrl+S

### Editing Existing Template

1. **Open Config Manager**
2. **Select template** → Double-click or Ctrl+D
3. **Modify fields** (asterisk appears in title)
4. **Save**: Ctrl+S or click Save button

## Integration

### Launched From
- **ManageConfigModern**: Double-click template or Edit button
- **ManageConfigModern**: Add Template button (new templates)

### Creates/Updates
- Template configuration in `config.properties` XML format
- All 17 template properties saved atomically

## Configuration Properties

Templates store these properties:

**Core:**
- `description` - Template description
- `docID` - Document identifier XPath

**Document:**
- `activite` - Activity code
- `typePiece` - Document type
- `typeJDE` - JDE type
- `societeJDE` - Company code

**Business Data:**
- `numClient` - Customer number XPath
- `montant` - Amount XPath
- `datePiece` - Document date XPath
- `dateEcheance` - Due date XPath

**Processing:**
- `transformYN` - Transform enabled (Y/N)
- `transform` - Transform XSL path
- `xsl` - Main XSL path
- `rtf` - RTF template path
- `burstKey` - Burst key XPath

**Routing:**
- `codeRoutage` - Routing code
- `numProc` - CPU count

## Tips

### XPath Best Practices
- Test XPath expressions on sample documents first
- Use absolute paths when possible (`/Invoice/ID`)
- Prefer unique identifiers for burst keys
- Handle missing elements gracefully

### Performance Tuning
- Single CPU for small documents (<10 pages)
- Multiple CPUs only useful in burst mode with many documents
- More CPUs ≠ always faster (overhead exists)
- Monitor system resources when adjusting

### Template Organization
- Use descriptive names (`invoice_fr`, `creditnote_ubl`)
- Document complex XPath in description field
- Test with sample documents before production
- Keep templates focused (one doc type per template)

## Status Messages

- **"Ready"** - Initial state, ready for input
- **"Template loaded"** - Existing template loaded successfully
- **"New template - configure and save"** - Creating new template
- **"Modified - remember to save"** - Unsaved changes exist
- **"Template saved successfully"** - Save completed
- **"Error saving template"** - Save failed (check logs)

## Error Handling

### Common Issues

**"Template name cannot be empty"**
- Provide a unique name before saving
- Focus returns to name field

**"A template with this name already exists"**
- Choose a different name
- Or edit existing template instead

**"Error opening template"**
- Check config file is valid XML
- Verify file permissions
- Check application logs

## Technical Details

- **Framework**: Java Swing with FlatLaf
- **Configuration**: Simple XML framework
- **Layout**: GridBagLayout with organized panels
- **File Format**: XML-based properties file
- **Validation**: Real-time with status feedback

## Comparison with Classic Editor

| Feature | Classic | Modern |
|---------|---------|--------|
| Layout | Single panel | 3 organized tabs |
| Help | None | Built-in tips panel |
| Tooltips | Minimal | Comprehensive |
| Status | Basic | Real-time feedback |
| Keyboard | None | Ctrl+S, Esc |
| Change tracking | No | Yes (asterisk) |
| Validation | Basic | Enhanced |
| Look | Windows 95 | FlatLaf modern |

## See Also

- [MODERNIZATION.md](MODERNIZATION.md) - Configuration Manager
- [MAIN_MODERN.md](MAIN_MODERN.md) - Main Interface
- [QUICK_START.txt](QUICK_START.txt) - Complete Quick Start
- [README.md](README.md) - Project Overview
