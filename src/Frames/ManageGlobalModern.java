/*
 * Copyright (c) 2018 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package Frames;

import custom.resources.Resource;
import custom.resources.Resources;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentListener;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Modern global configuration editor with organized tabs
 */
public class ManageGlobalModern extends JFrame {

    private String template;
    private String paramConfig;
    
    // UI Components
    private JTextField tName, tDescription;
    private JTextField tAppHome, tProcessHome, tTempDir, tOutputDir, tInputDir, tBurstingDir, tOutputDirE1;
    private JTextField tRunGS, tXSL1, tXSL2, tXSL3;
    private JTextArea taGhostscript, taJdbcString;
    private JTextField tSchemaDTA, tSchemaSVM, tUpdateDB, tTableLOG, tTableERR, tSetLocale;
    private JTextField tScpUser, tScpDirectory, tScpServer, tScpPasswd;
    private JTextField tDBUser, tDBPasswd, tXdoConfig, tDevMode;
    private JButton bSave, bCancel;
    private JLabel statusLabel;
    private JTabbedPane tabbedPane;
    private boolean modified = false;

    public ManageGlobalModern() {
        initModernComponents();
    }

    private void initModernComponents() {
        setTitle("Global Configuration - NomaUBL");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top - Header with name and description
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center - Tabbed configuration
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📁 Directories", createDirectoriesPanel());
        tabbedPane.addTab("🔧 Processing", createProcessingPanel());
        tabbedPane.addTab("💾 Database", createDatabasePanel());
        tabbedPane.addTab("🌐 Server/SFTP", createServerPanel());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Bottom - Actions and status
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setupKeyboardShortcuts();
        setupChangeListeners();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Template name (read-only)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblName = new JLabel("Template:");
        lblName.setFont(lblName.getFont().deriveFont(Font.BOLD));
        panel.add(lblName, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        tName = new JTextField();
        tName.setEditable(false);
        tName.setFont(tName.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(tName, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        tDescription = new JTextField();
        tDescription.setToolTipText("Brief description of this configuration");
        panel.add(tDescription, gbc);

        return panel;
    }

    private JPanel createDirectoriesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Application Home
        addField(fieldsPanel, gbc, row++, "Application Home:", 
                 tAppHome = createTextField("Root directory for application"), 
                 "Base path for the application (e.g., /opt/nomaubl)");

        // Process Home
        addField(fieldsPanel, gbc, row++, "Process Home:", 
                 tProcessHome = createTextField("Processing directory"), 
                 "Working directory for processing");

        // Separator
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        fieldsPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Input Directory
        addField(fieldsPanel, gbc, row++, "Input Directory:", 
                 tInputDir = createTextField("Input files location"), 
                 "Directory for input XML files");

        // Output Directory
        addField(fieldsPanel, gbc, row++, "Output Directory:", 
                 tOutputDir = createTextField("Output files location"), 
                 "Directory for generated output files");

        // Temp Directory
        addField(fieldsPanel, gbc, row++, "Temp Directory:", 
                 tTempDir = createTextField("Temporary files location"), 
                 "Temporary directory for processing");

        // Single Output Directory
        addField(fieldsPanel, gbc, row++, "Single Output Dir:", 
                 tOutputDirE1 = createTextField("Single mode output"), 
                 "Output directory for single mode processing");

        // Bursting Directory
        addField(fieldsPanel, gbc, row++, "Bursting Directory:", 
                 tBurstingDir = createTextField("Burst mode output"), 
                 "Output directory for burst mode processing");

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProcessingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // XDO Config
        addField(fieldsPanel, gbc, row++, "XDO Config:", 
                 tXdoConfig = createTextField("XDO configuration file"), 
                 "Path to XDO configuration file");

        // Routing XSL
        addField(fieldsPanel, gbc, row++, "Routing XSL:", 
                 tXSL1 = createTextField("Routing transformation"), 
                 "XSL for routing logic");

        // Copy XSL
        addField(fieldsPanel, gbc, row++, "Copy XSL:", 
                 tXSL2 = createTextField("Copy transformation"), 
                 "XSL for copy operations");

        // Dev XSL
        addField(fieldsPanel, gbc, row++, "Dev XSL:", 
                 tXSL3 = createTextField("Development transformation"), 
                 "XSL for development mode");

        // Separator
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        fieldsPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Ghostscript section
        JPanel gsPanel = new JPanel(new BorderLayout(5, 5));
        gsPanel.setBorder(createTitledBorder("Ghostscript Configuration"));

        JPanel gsTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gsTopPanel.add(new JLabel("Run Ghostscript:"));
        tRunGS = new JTextField(10);
        tRunGS.setToolTipText("Enable Ghostscript (Y/N)");
        gsTopPanel.add(tRunGS);
        gsPanel.add(gsTopPanel, BorderLayout.NORTH);

        taGhostscript = new JTextArea(4, 40);
        taGhostscript.setLineWrap(true);
        taGhostscript.setWrapStyleWord(true);
        taGhostscript.setToolTipText("Ghostscript command line");
        JScrollPane gsScroll = new JScrollPane(taGhostscript);
        gsPanel.add(gsScroll, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1;
        fieldsPanel.add(gsPanel, gbc);
        gbc.gridwidth = 1; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        // Separator
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        fieldsPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Dev Mode
        addField(fieldsPanel, gbc, row++, "Dev Mode:", 
                 tDevMode = createTextField("Development mode (Y/N)"), 
                 "Enable development mode");

        // Locale
        addField(fieldsPanel, gbc, row++, "Set Locale:", 
                 tSetLocale = createTextField("Locale setting"), 
                 "Locale for processing (e.g., en_US.UTF-8)");

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDatabasePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // JDBC Connection section
        JPanel jdbcPanel = new JPanel(new BorderLayout(5, 5));
        jdbcPanel.setBorder(createTitledBorder("JDBC Connection"));

        taJdbcString = new JTextArea(3, 40);
        taJdbcString.setLineWrap(true);
        taJdbcString.setWrapStyleWord(true);
        taJdbcString.setToolTipText("JDBC connection string");
        JScrollPane jdbcScroll = new JScrollPane(taJdbcString);
        jdbcPanel.add(jdbcScroll, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.3;
        fieldsPanel.add(jdbcPanel, gbc);
        gbc.gridwidth = 1; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        // Database credentials
        JPanel credPanel = new JPanel(new GridBagLayout());
        credPanel.setBorder(createTitledBorder("Database Credentials"));
        GridBagConstraints credGbc = new GridBagConstraints();
        credGbc.insets = new Insets(5, 5, 5, 5);
        credGbc.fill = GridBagConstraints.HORIZONTAL;

        credGbc.gridx = 0; credGbc.gridy = 0; credGbc.weightx = 0;
        credPanel.add(new JLabel("DB User:"), credGbc);
        credGbc.gridx = 1; credGbc.weightx = 1;
        tDBUser = createTextField("Database username");
        credPanel.add(tDBUser, credGbc);

        credGbc.gridx = 0; credGbc.gridy = 1; credGbc.weightx = 0;
        credPanel.add(new JLabel("DB Password:"), credGbc);
        credGbc.gridx = 1; credGbc.weightx = 1;
        tDBPasswd = new JPasswordField();
        tDBPasswd.setToolTipText("Database password");
        credPanel.add(tDBPasswd, credGbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        fieldsPanel.add(credPanel, gbc);
        gbc.gridwidth = 1;

        // Separator
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        fieldsPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Schema settings
        addField(fieldsPanel, gbc, row++, "Schema DTA:", 
                 tSchemaDTA = createTextField("Data schema"), 
                 "Database schema for data tables");

        addField(fieldsPanel, gbc, row++, "Schema SVM:", 
                 tSchemaSVM = createTextField("SVM schema"), 
                 "Database schema for SVM tables");

        // Database update
        addField(fieldsPanel, gbc, row++, "Update DB:", 
                 tUpdateDB = createTextField("Enable DB update (Y/N)"), 
                 "Enable database updates");

        // Separator
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        fieldsPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Table names
        addField(fieldsPanel, gbc, row++, "Log Table:", 
                 tTableLOG = createTextField("Logging table name"), 
                 "Table for logging information");

        addField(fieldsPanel, gbc, row++, "Error Table:", 
                 tTableERR = createTextField("Error table name"), 
                 "Table for error information");

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // SFTP Server settings
        JPanel sftpPanel = new JPanel(new GridBagLayout());
        sftpPanel.setBorder(createTitledBorder("SFTP Server Configuration"));
        GridBagConstraints sftpGbc = new GridBagConstraints();
        sftpGbc.insets = new Insets(5, 5, 5, 5);
        sftpGbc.fill = GridBagConstraints.HORIZONTAL;

        sftpGbc.gridx = 0; sftpGbc.gridy = 0; sftpGbc.weightx = 0;
        JLabel lblServer = new JLabel("Server:");
        lblServer.setPreferredSize(new Dimension(120, lblServer.getPreferredSize().height));
        sftpPanel.add(lblServer, sftpGbc);
        sftpGbc.gridx = 1; sftpGbc.weightx = 1.0;
        tScpServer = createTextField("SFTP server hostname or IP");
        sftpPanel.add(tScpServer, sftpGbc);

        sftpGbc.gridx = 0; sftpGbc.gridy = 1; sftpGbc.weightx = 0;
        JLabel lblUser = new JLabel("User:");
        lblUser.setPreferredSize(new Dimension(120, lblUser.getPreferredSize().height));
        sftpPanel.add(lblUser, sftpGbc);
        sftpGbc.gridx = 1; sftpGbc.weightx = 1.0;
        tScpUser = createTextField("SFTP username");
        sftpPanel.add(tScpUser, sftpGbc);

        sftpGbc.gridx = 0; sftpGbc.gridy = 2; sftpGbc.weightx = 0;
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setPreferredSize(new Dimension(120, lblPassword.getPreferredSize().height));
        sftpPanel.add(lblPassword, sftpGbc);
        sftpGbc.gridx = 1; sftpGbc.weightx = 1.0;
        tScpPasswd = new JPasswordField();
        tScpPasswd.setToolTipText("SFTP password (encoded)");
        sftpPanel.add(tScpPasswd, sftpGbc);

        sftpGbc.gridx = 0; sftpGbc.gridy = 3; sftpGbc.weightx = 0;
        JLabel lblDirectory = new JLabel("Directory:");
        lblDirectory.setPreferredSize(new Dimension(120, lblDirectory.getPreferredSize().height));
        sftpPanel.add(lblDirectory, sftpGbc);
        sftpGbc.gridx = 1; sftpGbc.weightx = 1.0;
        tScpDirectory = createTextField("Remote directory path");
        sftpPanel.add(tScpDirectory, sftpGbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3; gbc.weightx = 1.0;
        fieldsPanel.add(sftpPanel, gbc);
        gbc.gridwidth = 1;

        // Spacer
        gbc.gridy = row++; gbc.weighty = 1;
        fieldsPanel.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        
        bSave = new JButton("💾 Save");
        bSave.setToolTipText("Save configuration (Ctrl+S)");
        bSave.setPreferredSize(new Dimension(100, 32));
        bSave.addActionListener(e -> saveConfiguration());
        
        bCancel = new JButton("Cancel");
        bCancel.setToolTipText("Close without saving (Esc)");
        bCancel.setPreferredSize(new Dimension(100, 32));
        bCancel.addActionListener(e -> cancelAndClose());

        buttonPanel.add(bSave);
        buttonPanel.add(bCancel);

        panel.add(statusPanel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, 
                         JTextField field, String tooltip) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        field.setToolTipText(tooltip);
        panel.add(field, gbc);
    }

    private JTextField createTextField(String tooltip) {
        JTextField field = new JTextField();
        field.setToolTipText(tooltip);
        return field;
    }

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP
        );
    }

    private void setupKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+S - Save
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuKey), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfiguration();
            }
        });

        // Esc - Cancel
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        actionMap.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelAndClose();
            }
        });
    }

    private void setupChangeListeners() {
        DocumentListener docListener = new DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { setModified(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { setModified(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { setModified(); }
        };

        // Add listeners to all text fields
        Component[] components = {tDescription, tAppHome, tProcessHome, tTempDir, 
            tOutputDir, tInputDir, tBurstingDir, tOutputDirE1, tRunGS, tXSL1, tXSL2, tXSL3,
            tSchemaDTA, tSchemaSVM, tUpdateDB, tTableLOG, tTableERR, tSetLocale,
            tScpUser, tScpDirectory, tScpServer, tScpPasswd, tDBUser, tDBPasswd, 
            tXdoConfig, tDevMode};
        
        for (Component comp : components) {
            if (comp instanceof JTextField) {
                ((JTextField) comp).getDocument().addDocumentListener(docListener);
            }
        }
        
        taGhostscript.getDocument().addDocumentListener(docListener);
        taJdbcString.getDocument().addDocumentListener(docListener);
    }

    private void setModified() {
        if (!modified) {
            modified = true;
            setTitle("* " + getTitle());
            statusLabel.setText("Modified - remember to save");
        }
    }

    private void saveConfiguration() {
        try {
            File file = new File(paramConfig);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);
            Resource resource = resources.getResourceByName(template);

            // Save all fields
            resource.setProperty("description", tDescription.getText());
            resource.setProperty("appHome", tAppHome.getText());
            resource.setProperty("processHome", tProcessHome.getText());
            resource.setProperty("tempOutput", tTempDir.getText());
            resource.setProperty("dirOutput", tOutputDir.getText());
            resource.setProperty("dirInput", tInputDir.getText());
            resource.setProperty("burstOutput", tBurstingDir.getText());
            resource.setProperty("singleOutput", tOutputDirE1.getText());
            resource.setProperty("runGS", tRunGS.getText());
            resource.setProperty("cmdGS", taGhostscript.getText());
            resource.setProperty("routageXSL", tXSL1.getText());
            resource.setProperty("copyXSL", tXSL2.getText());
            resource.setProperty("URL", taJdbcString.getText());
            resource.setProperty("schema", tSchemaDTA.getText());
            resource.setProperty("schemaSVM", tSchemaSVM.getText());
            resource.setProperty("updateDB", tUpdateDB.getText());
            resource.setProperty("tableLog", tTableLOG.getText());
            resource.setProperty("tableErr", tTableERR.getText());
            resource.setProperty("setLocale", tSetLocale.getText());
            resource.setProperty("scpUser", tScpUser.getText());
            resource.setProperty("scpDir", tScpDirectory.getText());
            resource.setProperty("scpServer", tScpServer.getText());
            resource.setProperty("scpPassword", tScpPasswd.getText());
            resource.setProperty("DBUser", tDBUser.getText());
            resource.setProperty("DBPassword", tDBPasswd.getText());
            resource.setProperty("xdo", tXdoConfig.getText());
            resource.setProperty("devModeYN", tDevMode.getText());
            resource.setProperty("devXSL", tXSL3.getText());

            serializer.write(resources, file);

            modified = false;
            setTitle(getTitle().replace("* ", ""));
            statusLabel.setText("Configuration saved successfully");
            
            JOptionPane.showMessageDialog(this,
                "Global configuration saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);

            dispose();

        } catch (Exception ex) {
            statusLabel.setText("Error saving configuration");
            JOptionPane.showMessageDialog(this,
                "Error saving configuration:\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void cancelAndClose() {
        if (modified) {
            int result = JOptionPane.showConfirmDialog(this,
                "You have unsaved changes. Are you sure you want to close?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }

    public void GlobalInit(String pTemplate, String pConfig) throws Exception {
        template = pTemplate;
        tName.setText(template);
        paramConfig = pConfig;

        File file = new File(paramConfig);
        Serializer serializer = new Persister();
        Resources resources = serializer.read(Resources.class, file);
        Resource resource = resources.getResourceByName(template);

        // Load all fields
        tDescription.setText(resource.getProperty("description"));
        tAppHome.setText(resource.getProperty("appHome"));
        tProcessHome.setText(resource.getProperty("processHome"));
        tTempDir.setText(resource.getProperty("tempOutput"));
        tOutputDir.setText(resource.getProperty("dirOutput"));
        tInputDir.setText(resource.getProperty("dirInput"));
        tBurstingDir.setText(resource.getProperty("burstOutput"));
        tOutputDirE1.setText(resource.getProperty("singleOutput"));
        tRunGS.setText(resource.getProperty("runGS"));
        taGhostscript.setText(resource.getProperty("cmdGS"));
        tXSL1.setText(resource.getProperty("routageXSL"));
        tXSL2.setText(resource.getProperty("copyXSL"));
        taJdbcString.setText(resource.getProperty("URL"));
        tSchemaDTA.setText(resource.getProperty("schema"));
        tSchemaSVM.setText(resource.getProperty("schemaSVM"));
        tUpdateDB.setText(resource.getProperty("updateDB"));
        tTableLOG.setText(resource.getProperty("tableLog"));
        tTableERR.setText(resource.getProperty("tableErr"));
        tSetLocale.setText(resource.getProperty("setLocale"));
        tScpUser.setText(resource.getProperty("scpUser"));
        tScpDirectory.setText(resource.getProperty("scpDir"));
        tScpServer.setText(resource.getProperty("scpServer"));
        tScpPasswd.setText(resource.getProperty("scpPassword"));
        tDBUser.setText(resource.getProperty("DBUser"));
        tDBPasswd.setText(resource.getProperty("DBPassword"));
        tXdoConfig.setText(resource.getProperty("xdo"));
        tDevMode.setText(resource.getProperty("devModeYN"));
        tXSL3.setText(resource.getProperty("devXSL"));

        // Reset modified flag after loading
        modified = false;
        statusLabel.setText("Configuration loaded");
    }

    public static void main(String[] args) {
        // Set VS Code dark theme
        DarkTheme.apply();

        SwingUtilities.invokeLater(() -> {
            ManageGlobalModern frame = new ManageGlobalModern();
            frame.setVisible(true);
        });
    }
}
