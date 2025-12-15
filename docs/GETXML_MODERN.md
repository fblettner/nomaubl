# GetXMLModern - File Download Interface

A modern interface for downloading XML files from BI Publisher server via SFTP with async operations and status feedback.

## Overview

GetXMLModern provides a clean, user-friendly interface for retrieving XML files from the BI Publisher server. It uses SFTP to securely download files based on report information, with real-time progress and status updates.

## Interface Layout

### Header Section
- 📥 **Title**: "Download XML File from Server"
- **Description**: Brief explanation of the functionality

### File Information Panel

All fields are clearly labeled with helpful tooltips:

- **Report**: Report name (e.g., `R42565`, `INVOICE`)
- **Version**: Report version (e.g., `FBL0001`, `V001`)
- **Language**: Language code (default: `FR`)
- **Job Number**: BI Publisher job number

**File Pattern Display**: Shows the constructed filename pattern:
```
Report_Version_Language_JobNumber.xml
```

### Bottom Section
- **Progress Bar**: Animated during download (hidden when idle)
- **Status Label**: Real-time status messages
- **Download Button**: Large, prominent "📥 Download File" button
- **Close Button**: Standard close action

## Features

### Input Validation
- ✅ Required field checking
- ✅ Focus on first invalid field
- ✅ User-friendly error messages

### Async Download
- ⚡ Non-blocking SFTP operations
- 📊 Progress indicator during download
- 🔄 UI disabled during operation
- ✅ Success/error notifications

### Status Feedback
Real-time status messages:
- "Ready to download - enter file information"
- "Connecting to server..."
- "Downloading file..."
- "✅ File downloaded successfully: filename.xml"
- "❌ Download failed"

### Error Handling
Comprehensive error messages for:
- Connection errors
- Authentication failures
- File not found
- Network issues
- Permission problems

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| **Ctrl+D** | Download file |
| **Enter** | Download file (when fields filled) |
| **Esc** | Close window |

## Usage Example

### Download an Invoice XML

1. **Launch Interface**:
   ```bash
   ./test/run_modern_getxml.sh ./test/config/config.properties
   ```

2. **Enter File Information**:
   - Report: `R42565`
   - Version: `FBL0001`
   - Language: `FR`
   - Job Number: `123456`

3. **Download**:
   - Click "Download File" or press **Ctrl+D**
   - Watch progress bar
   - Wait for success message

4. **Result**:
   - File: `R42565_FBL0001_FR_123456.xml`
   - Location: Configured input directory
   - Status: "✅ File downloaded successfully"

## Configuration

The interface reads SFTP settings from the global configuration:

### Required Properties
```properties
scpDir=/path/to/remote/directory/
scpUser=username
scpServer=server.example.com
scpPassword=encrypted_or_plain
dirInput=/path/to/local/input/
```

### Property Details

- **scpDir**: Remote directory on BI Publisher server
- **scpUser**: SFTP username
- **scpServer**: Server hostname or IP
- **scpPassword**: SFTP password (stored in config)
- **dirInput**: Local directory for downloaded files

**Security Note**: Consider using SSH keys instead of password authentication for production environments.

## Technical Details

### SFTP Connection
- **Protocol**: SFTP (SSH File Transfer Protocol)
- **Port**: 22 (standard SSH)
- **Library**: JSch (Java Secure Channel)
- **Security**: StrictHostKeyChecking disabled (configurable)

### Async Operation
```java
SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
    @Override
    protected Boolean doInBackground() {
        // SFTP download in background thread
        // UI remains responsive
    }
    
    @Override
    protected void done() {
        // Update UI on completion
        // Show success/error message
    }
};
```

### File Naming Convention
```
{Report}_{Version}_{Language}_{JobNumber}.xml

Examples:
- R42565_FBL0001_FR_123456.xml
- INVOICE_V001_EN_789012.xml
- CREDITNOTE_V002_DE_345678.xml
```

## Integration

### Standalone Usage
```bash
# With default config
./test/run_modern_getxml.sh

# With custom config
./test/run_modern_getxml.sh /path/to/config.properties
```

### Programmatic Usage
```java
GetXMLModern frame = new GetXMLModern();
frame.GetXMLInit("/path/to/config.properties");
frame.setVisible(true);
```

### From Main Interface
- Integrated in MainModern Tab 2
- Shared config and logic
- Consistent UX across interfaces

## Comparison with Classic GetXML

| Feature | Classic | Modern |
|---------|---------|--------|
| Layout | GroupLayout | GridBagLayout + BorderLayout |
| Design | Nimbus | FlatLaf modern |
| Feedback | Minimal | Progress bar + status |
| Async | ❌ Blocking | ✅ Non-blocking |
| Validation | ❌ None | ✅ Comprehensive |
| Shortcuts | ❌ None | ✅ Full keyboard |
| Error messages | Generic | User-friendly |
| Status updates | ❌ None | ✅ Real-time |
| Progress | ❌ No indicator | ✅ Visual progress bar |
| Tooltips | Basic | Comprehensive |

## Error Scenarios

### Connection Failed
```
❌ Download failed
Connection error: Connection refused

Possible causes:
- Server unreachable
- Wrong hostname/IP
- Firewall blocking port 22
- Server down
```

### Authentication Failed
```
❌ Download failed
Connection error: Auth fail

Possible causes:
- Wrong username
- Wrong password
- Account locked
- SSH key mismatch
```

### File Not Found
```
❌ Download failed
File transfer error: No such file

Possible causes:
- Wrong job number
- File not yet generated
- Wrong remote directory
- File already archived
```

### Permission Denied
```
❌ Download failed
File transfer error: Permission denied

Possible causes:
- No read permission on remote file
- No write permission on local directory
- Disk full
```

## Best Practices

### Security
- ✅ Use encrypted passwords in config
- ✅ Consider SSH key authentication
- ✅ Restrict SFTP user permissions
- ✅ Use separate SFTP account (not root)
- ✅ Monitor failed login attempts

### Performance
- ✅ Download during off-peak hours for large files
- ✅ Check network connectivity first
- ✅ Keep local directory clean (archive old files)
- ✅ Use fast local storage (SSD)

### Reliability
- ✅ Verify job number before downloading
- ✅ Check file exists on server first (if possible)
- ✅ Handle network interruptions gracefully
- ✅ Log all downloads for audit trail
- ✅ Clean up failed partial downloads

## Troubleshooting

### Problem: "Please enter a job number!"
**Solution**: Fill in all required fields before clicking Download

### Problem: Connection timeout
**Solution**: 
- Check network connectivity
- Verify server is running
- Check firewall rules
- Try pinging the server first

### Problem: Authentication keeps failing
**Solution**:
- Verify credentials in config file
- Check if password is encrypted correctly
- Test credentials with SFTP client
- Contact server administrator

### Problem: File downloads but is corrupted
**Solution**:
- Check disk space on local machine
- Verify file permissions
- Check for network instability
- Re-download the file

### Problem: Downloaded file goes to wrong location
**Solution**:
- Check `dirInput` property in config
- Verify directory exists and is writable
- Check for `%TEMPLATE%` placeholder replacement
- Use absolute paths

## Status Messages Reference

| Message | Meaning |
|---------|---------|
| "Ready to download - enter file information" | Initial state |
| "Connecting to server..." | Establishing SFTP connection |
| "Downloading file..." | Transfer in progress |
| "✅ File downloaded successfully: {file}" | Download completed |
| "❌ Download failed" | Error occurred |

## Logging

The interface logs errors at SEVERE level:
- Connection failures
- Authentication errors
- File transfer errors
- Configuration issues

Check logs for detailed error information:
```bash
# View recent errors
tail -f application.log | grep SEVERE
```

## Future Enhancements

### Potential Improvements
- 🔄 Retry mechanism for failed downloads
- 📁 Browse for local destination
- 📜 Download history/log viewer
- 🔍 Search/browse remote directory
- 📊 Transfer speed indicator
- ⏸️ Pause/resume large downloads
- 🔐 SSH key authentication
- 📋 Multiple file download queue

## See Also

- [MAIN_MODERN.md](MAIN_MODERN.md) - Main interface (includes Get File tab)
- [MODERNIZATION_SUMMARY.md](MODERNIZATION_SUMMARY.md) - All interfaces overview
- [README.md](README.md) - Project overview
- [QUICK_START.txt](QUICK_START.txt) - Quick start guide

---

**Copyright © 2018-2025 NOMANA-IT**  
Subject to license terms
