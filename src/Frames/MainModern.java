/*
 * Copyright (c) 2018 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package Frames;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import custom.resources.Resource;
import custom.resources.Resources;
import custom.resources.Template;
import custom.ubl.UBLValidator;
import custom.ubl.ValidationError;
import custom.ubl.ValidationResult;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.w3c.dom.Document;
import static custom.resources.Tools.decodePasswd;
import static custom.resources.Tools.infoBox;
import static nomaubl.ScheduleUBL.GenerateReport;

/**
 * Modern main interface with FlatLaf UI
 */
public class MainModern extends JFrame {

    private String paramFile;
    private String paramConfig;
    private static String pAppHome;
    private static String pProcessHome;
    private String defaultInputDir;
    
    private final static String TEMPLATE = "%TEMPLATE%";
    private final static String FILE_NAME = "%FILE_NAME%";
    private final static String APP_HOME = "%APP_HOME%";
    private final static String PROCESS_HOME = "%PROCESS_HOME%";

    // UI Components
    private JComboBox<String> cbTemplate;
    private JComboBox<String> cbMode;
    private JTextField txFileName;
    private JTextField tReport, tVersion, tLanguage, tJobNumber, txServerOutputPath;
    private JButton bSettings, bExit, bInputFile, bGenerateReport, bGetFile, bBrowseServerOutput;
    private JLabel statusLabel;
    private JTabbedPane tabbedPane;
    
    // UBL Validation components
    private JTextField txUblFileName;
    private JButton bUblInputFile, bValidateUbl;
    private JTable tableUblResults;
    private DefaultTableModel tableModelUblResults;
    private JRadioButton rbSourceXml, rbSourceUbl;
    private ButtonGroup bgSourceType;
    
    // Generate Report validation results
    private JTable tableGenerateResults;
    private DefaultTableModel tableModelGenerateResults;
    
    // Database XML extraction components
    private JTextField txDbFedoc, txDbFedct, txDbKco, txDbOutputPath;
    private JButton bExtractXml, bBrowseDbOutput;
    private JTextArea txDbResults;

    public MainModern() {
        initModernComponents();
    }

    private void initModernComponents() {
        setTitle("NomaUBL - UBL Report Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 750);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top area - Welcome message and toolbar
        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        
        // Welcome message
        JPanel welcomePanel = createWelcomePanel();
        topArea.add(welcomePanel, BorderLayout.NORTH);
        
        // Toolbar
        JPanel toolbarPanel = createToolbar();
        topArea.add(toolbarPanel, BorderLayout.SOUTH);
        
        mainPanel.add(topArea, BorderLayout.NORTH);

        // Center - Tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📄 Generate Report", createGeneratePanel());
        tabbedPane.addTab("✓ UBL Validation", createUblValidationPanel());
        tabbedPane.addTab("💾 Extract XML from Database", createDbExtractPanel());
        tabbedPane.addTab("📥 Download XML File from Server", createGetFilePanel());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Bottom status bar
        JPanel statusPanel = createStatusBar();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setupKeyboardShortcuts();
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Welcome text
        JLabel welcomeLabel = new JLabel("<html><div style='padding: 10px;'>" +
                "<span style='font-size: 16px; font-weight: bold;'>🎯 Welcome to NomaUBL</span><br>" +
                "<span style='font-size: 11px; color: #808080;'>Universal Business Language Report Generator & Validator</span>" +
                "</div></html>");
        panel.add(welcomeLabel, BorderLayout.WEST);
        
        return panel;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Left side - Template selector
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel lblTemplate = new JLabel("📋 Template:");
        cbTemplate = new JComboBox<>();
        cbTemplate.setPreferredSize(new Dimension(200, 30));
        cbTemplate.setToolTipText("Select report template");
        leftPanel.add(lblTemplate);
        leftPanel.add(cbTemplate);

        // Right side - Action buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        bSettings = createButton("⚙️ Settings", "Open configuration manager (Ctrl+,)");
        bSettings.addActionListener(e -> openSettings());
        
        bExit = createButton("↪ Exit", "Exit application (Ctrl+Q)");
        bExit.addActionListener(e -> System.exit(0));

        rightPanel.add(bSettings);
        rightPanel.add(bExit);

        toolbar.add(leftPanel, BorderLayout.WEST);
        toolbar.add(rightPanel, BorderLayout.EAST);

        return toolbar;
    }

    private JPanel createGeneratePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Input section
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(createTitledBorder("Input Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // File selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        inputPanel.add(new JLabel("Input File:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        txFileName = new JTextField();
        txFileName.setEditable(false);
        txFileName.setPreferredSize(new Dimension(300, 30));
        inputPanel.add(txFileName, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        bInputFile = createButton("📁 Browse...", "Select input XML file");
        bInputFile.addActionListener(e -> selectInputFile());
        inputPanel.add(bInputFile, gbc);

        // Mode selection
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Mode:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        cbMode = new JComboBox<>(new String[]{
            "SINGLE - Single document processing",
            "BURST - Multiple documents burst processing",
            "UBL - UBL XML only generation",
            "BOTH - UBL XML + PDF + XML generation"
        });
        cbMode.setPreferredSize(new Dimension(350, 30));
        cbMode.setToolTipText("Select processing mode");
        inputPanel.add(cbMode, gbc);
        gbc.gridwidth = 1;

        panel.add(inputPanel, BorderLayout.NORTH);

        // Action button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        bGenerateReport = createButton("📄 Generate Report", "Generate UBL report (Ctrl+G)");
        bGenerateReport.setPreferredSize(new Dimension(180, 40));
        bGenerateReport.addActionListener(e -> generateReport());
        actionPanel.add(bGenerateReport);

        // Results area - Table (for UBL/BOTH modes)
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(createTitledBorder("Validation Results"));
        
        String[] columnNames = {"Severity", "Source", "Rule ID", "Message"};
        tableModelGenerateResults = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableGenerateResults = new JTable(tableModelGenerateResults);
        tableGenerateResults.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableGenerateResults.getColumnModel().getColumn(0).setPreferredWidth(100);  // Severity
        tableGenerateResults.getColumnModel().getColumn(1).setPreferredWidth(150); // Source
        tableGenerateResults.getColumnModel().getColumn(2).setPreferredWidth(200); // Rule ID
        tableGenerateResults.getColumnModel().getColumn(3).setPreferredWidth(550); // Message
        tableGenerateResults.getTableHeader().setReorderingAllowed(false);
        
        // Use HTML renderer for multi-line display
        tableGenerateResults.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (value != null) {
                    String text = value.toString();
                    setText("<html><div style='width: " + (table.getColumnModel().getColumn(column).getWidth() - 10) + "px; word-wrap: break-word; overflow-wrap: break-word;'>" 
                        + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("{", "&#123;").replace("}", "&#125;").replace("'", "&#39;").replace("\n", "<br>") 
                        + "</div></html>");
                }
                
                // Color coding based on severity
                if (column == 0 && value != null) {
                    String severity = value.toString();
                    if (severity.equals("ERROR") || severity.equals("FATAL")) {
                        setForeground(new Color(220, 53, 69));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (severity.equals("WARNING")) {
                        setForeground(new Color(255, 193, 7));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (severity.equals("SUCCESS")) {
                        setForeground(new Color(40, 167, 69));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(table.getForeground());
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                } else {
                    setForeground(table.getForeground());
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(table.getBackground());
                }
                
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(tableGenerateResults);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        // Container for action and results
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(actionPanel, BorderLayout.NORTH);
        centerPanel.add(resultsPanel, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createUblValidationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top input section
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(createTitledBorder("Input File"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Source type selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        inputPanel.add(new JLabel("Source Type:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0; gbc.gridwidth = 2;
        JPanel sourceTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rbSourceXml = new JRadioButton("XML (transform to UBL)", true);
        rbSourceUbl = new JRadioButton("UBL (validate directly)");
        bgSourceType = new ButtonGroup();
        bgSourceType.add(rbSourceXml);
        bgSourceType.add(rbSourceUbl);
        sourceTypePanel.add(rbSourceXml);
        sourceTypePanel.add(rbSourceUbl);
        inputPanel.add(sourceTypePanel, gbc);

        // File selection
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Input File:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        txUblFileName = new JTextField();
        txUblFileName.setEditable(false);
        txUblFileName.setPreferredSize(new Dimension(300, 30));
        inputPanel.add(txUblFileName, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        bUblInputFile = createButton("📁 Browse...", "Select input file (XML or UBL)");
        bUblInputFile.addActionListener(e -> selectUblInputFile());
        inputPanel.add(bUblInputFile, gbc);

        // Validate button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        bValidateUbl = createButton("✓ Validate UBL", "Validate UBL document (Ctrl+V)");
        bValidateUbl.setPreferredSize(new Dimension(180, 40));
        bValidateUbl.addActionListener(e -> validateUbl());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(bValidateUbl);
        inputPanel.add(buttonPanel, gbc);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Results area - Table
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(createTitledBorder("Validation Results"));
        
        String[] columnNames = {"Severity", "Source", "Rule ID", "Message"};
        tableModelUblResults = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableUblResults = new JTable(tableModelUblResults);
        tableUblResults.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableUblResults.getColumnModel().getColumn(0).setPreferredWidth(100);  // Severity
        tableUblResults.getColumnModel().getColumn(1).setPreferredWidth(150); // Source
        tableUblResults.getColumnModel().getColumn(2).setPreferredWidth(200); // Rule ID
        tableUblResults.getColumnModel().getColumn(3).setPreferredWidth(550); // Message
        tableUblResults.getTableHeader().setReorderingAllowed(false);
        
        // Use HTML renderer for multi-line display
        tableUblResults.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                String text = value != null ? value.toString() : "";
                // Escape HTML first, then replace newlines with HTML breaks
                text = text.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;")
                          .replace("{", "&#123;")
                          .replace("}", "&#125;")
                          .replace("'", "&#39;")
                          .replace("\n", "<br>");
                text = "<html><div style='width: " + (table.getColumnModel().getColumn(column).getWidth() - 10) + "px; word-wrap: break-word; overflow-wrap: break-word;'>" 
                       + text + "</div></html>";
                
                Component c = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    String severity = (String) table.getValueAt(row, 0);
                    switch (severity.toUpperCase()) {
                        case "FATAL":
                        case "ERROR":
                            c.setBackground(DarkTheme.VALIDATION_ERROR_BG);
                            c.setForeground(DarkTheme.VALIDATION_ERROR_FG);
                            break;
                        case "WARNING":
                            c.setBackground(DarkTheme.VALIDATION_WARNING_BG);
                            c.setForeground(DarkTheme.VALIDATION_WARNING_FG);
                            break;
                        case "SUCCESS":
                            c.setBackground(DarkTheme.VALIDATION_SUCCESS_BG);
                            c.setForeground(DarkTheme.VALIDATION_SUCCESS_FG);
                            break;
                        default:
                            c.setBackground(UIManager.getColor("Table.background"));
                            c.setForeground(UIManager.getColor("Table.foreground"));
                            break;
                    }
                } else {
                    c.setBackground(UIManager.getColor("Table.selectionBackground"));
                    c.setForeground(UIManager.getColor("Table.selectionForeground"));
                }
                
                ((JLabel) c).setVerticalAlignment(SwingConstants.TOP);
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(tableUblResults);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(resultsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDbExtractPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top input section
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(createTitledBorder("Database Query Parameters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // FEDOC field
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        inputPanel.add(new JLabel("FEDOC:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txDbFedoc = new JTextField();
        txDbFedoc.setPreferredSize(new Dimension(200, 30));
        txDbFedoc.setToolTipText("Document ID");
        inputPanel.add(txDbFedoc, gbc);

        // FEDCT field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        inputPanel.add(new JLabel("FEDCT:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txDbFedct = new JTextField();
        txDbFedct.setPreferredSize(new Dimension(200, 30));
        txDbFedct.setToolTipText("Document Type");
        inputPanel.add(txDbFedct, gbc);

        // FEKCO field
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        inputPanel.add(new JLabel("FEKCO:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txDbKco = new JTextField();
        txDbKco.setPreferredSize(new Dimension(200, 30));
        txDbKco.setToolTipText("Company Code");
        inputPanel.add(txDbKco, gbc);

        // Output directory
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        inputPanel.add(new JLabel("Output Directory:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txDbOutputPath = new JTextField();
        txDbOutputPath.setEditable(false);
        txDbOutputPath.setPreferredSize(new Dimension(300, 30));
        inputPanel.add(txDbOutputPath, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        bBrowseDbOutput = createButton("📁 Browse...", "Select output directory");
        bBrowseDbOutput.addActionListener(e -> selectDbOutputDirectory());
        inputPanel.add(bBrowseDbOutput, gbc);

        // Extract button
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        bExtractXml = createButton("💾 Extract XML", "Extract XML from database (Ctrl+E)");
        bExtractXml.setPreferredSize(new Dimension(180, 40));
        bExtractXml.addActionListener(e -> extractXmlFromDatabase());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(bExtractXml);
        inputPanel.add(buttonPanel, gbc);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Results area
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(createTitledBorder("Extraction Results"));
        
        txDbResults = new JTextArea();
        txDbResults.setEditable(false);
        txDbResults.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txDbResults.setLineWrap(true);
        txDbResults.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(txDbResults);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(resultsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGetFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Input fields
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(createTitledBorder("Server File Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Report
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        inputPanel.add(new JLabel("Report:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tReport = new JTextField();
        tReport.setPreferredSize(new Dimension(200, 30));
        inputPanel.add(tReport, gbc);

        // Version
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        inputPanel.add(new JLabel("Version:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tVersion = new JTextField();
        tVersion.setPreferredSize(new Dimension(200, 30));
        inputPanel.add(tVersion, gbc);

        // Language
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Language:"), gbc);
        gbc.gridx = 1;
        tLanguage = new JTextField("FR");
        tLanguage.setPreferredSize(new Dimension(200, 30));
        tLanguage.setToolTipText("Language code (e.g., FR, EN)");
        inputPanel.add(tLanguage, gbc);

        // Job Number
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Job Number:"), gbc);
        gbc.gridx = 1;
        tJobNumber = new JTextField();
        tJobNumber.setPreferredSize(new Dimension(200, 30));
        inputPanel.add(tJobNumber, gbc);

        // Output directory
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        inputPanel.add(new JLabel("Output Directory:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txServerOutputPath = new JTextField();
        txServerOutputPath.setEditable(false);
        txServerOutputPath.setPreferredSize(new Dimension(300, 30));
        inputPanel.add(txServerOutputPath, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        bBrowseServerOutput = createButton("📁 Browse...", "Select output directory");
        bBrowseServerOutput.addActionListener(e -> selectServerOutputDirectory());
        inputPanel.add(bBrowseServerOutput, gbc);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Action button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        bGetFile = createButton("📥 Download File", "Download file from server (Ctrl+D)");
        bGetFile.setPreferredSize(new Dimension(180, 40));
        bGetFile.addActionListener(e -> {
            try {
                getFile();
            } catch (Exception ex) {
                showError("Error downloading file", ex);
            }
        });
        actionPanel.add(bGetFile);

        panel.add(actionPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("v2025.1 | © NOMANA-IT");
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.PLAIN, 10f));
        versionLabel.setForeground(Color.GRAY);
        statusBar.add(versionLabel, BorderLayout.EAST);

        return statusBar;
    }

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP
        );
    }

    private JButton createButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        
        // Modern button styling with custom rounded corners
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 32));
        
        return button;
    }

    private void setupKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+, - Settings
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, menuKey), "settings");
        actionMap.put("settings", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettings();
            }
        });

        // Ctrl+Q - Exit
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, menuKey), "exit");
        actionMap.put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        // Ctrl+G - Generate
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, menuKey), "generate");
        actionMap.put("generate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    generateReport();
                }
            }
        });

        // Ctrl+V - Validate UBL
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuKey), "validate");
        actionMap.put("validate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 1) {
                    validateUbl();
                }
            }
        });

        // Ctrl+E - Extract from Database
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, menuKey), "extract");
        actionMap.put("extract", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 2) {
                    extractXmlFromDatabase();
                }
            }
        });

        // Ctrl+D - Download
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, menuKey), "download");
        actionMap.put("download", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 3) {
                    try {
                        getFile();
                    } catch (Exception ex) {
                        showError("Error downloading file", ex);
                    }
                }
            }
        });

        // Ctrl+O - Open file
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuKey), "open");
        actionMap.put("open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectInputFile();
            }
        });
    }

    private void openSettings() {
        try {
            ManageConfigModern manageConfig = new ManageConfigModern();
            if (paramConfig != null && !paramConfig.isEmpty()) {
                manageConfig.setDefaultConfigFile(paramConfig);
            }
            manageConfig.setVisible(true);
        } catch (Exception e) {
            showError("Error opening settings", e);
        }
    }

    private void selectInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith(".xml");
            }

            @Override
            public String getDescription() {
                return "XML files (*.xml)";
            }
        });
        
        // Use default input directory if available
        String startDir = defaultInputDir != null ? defaultInputDir : System.getProperty("user.dir");
        chooser.setCurrentDirectory(new File(startDir));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileInput = chooser.getSelectedFile();
            txFileName.setText(fileInput.getAbsolutePath());
            paramFile = FilenameUtils.getBaseName(fileInput.getName());
            statusLabel.setText("Selected: " + fileInput.getName());
        }
    }

    private void generateReport() {
        if (txFileName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select an input file first.",
                "Input Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cbTemplate.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a template.",
                "Template Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusLabel.setText("Generating report...");
        tableModelGenerateResults.setRowCount(0);
        bGenerateReport.setEnabled(false);
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                File file = new File(paramConfig);
                Serializer serializer = new Persister();
                Resources resources = serializer.read(Resources.class, file);

                Resource resource = resources.getResourceByName("global");
                pAppHome = resource.getProperty("appHome");
                pProcessHome = resource.getProperty("processHome");
                String pDirInput = replaceConstValue(resource.getProperty("dirInput"));
                String inputFile = pDirInput + paramFile + ".xml";

                FileUtils.copyFile(new File(txFileName.getText()), new File(inputFile));

                // Extract template name (before " - ")
                String template = cbTemplate.getSelectedItem().toString();
                if (template.contains(" - ")) {
                    template = template.substring(0, template.indexOf(" - "));
                }
                
                // Extract mode (before " - ")
                String mode = cbMode.getSelectedItem().toString();
                if (mode.contains(" - ")) {
                    mode = mode.substring(0, mode.indexOf(" - "));
                }
                
                // Capture output for UBL/BOTH modes
                if (mode.equals("UBL") || mode.equals("BOTH") || mode.equals("BURST")) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    java.io.PrintStream ps = new java.io.PrintStream(baos);
                    java.io.PrintStream oldOut = System.out;
                    java.io.PrintStream oldErr = System.err;
                    System.setOut(ps);
                    System.setErr(ps);
                    
                    try {
                        GenerateReport(template, paramFile, mode, "1", paramConfig, true);
                        System.out.flush();
                        System.err.flush();
                        System.setOut(oldOut);
                        System.setErr(oldErr);
                        return baos.toString();
                    } catch (Exception e) {
                        System.setOut(oldOut);
                        System.setErr(oldErr);
                        return "ERROR:" + e.getMessage() + "\n" + baos.toString();
                    }
                } else {
                    GenerateReport(template, paramFile, mode, "1", paramConfig, true);
                    return "SUCCESS";
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    
                    // Extract mode to check if we should parse validation results
                    String mode = cbMode.getSelectedItem().toString();
                    if (mode.contains(" - ")) {
                        mode = mode.substring(0, mode.indexOf(" - "));
                    }
                    
                    if (mode.equals("UBL") || mode.equals("BOTH") || mode.equals("BURST")) {
                        // Parse and display validation results
                        tableModelGenerateResults.setRowCount(0);
                        
                        if (result.startsWith("ERROR:")) {
                            statusLabel.setText("✗ Generation failed");
                            String[] errorParts = result.substring(6).split("\n", 2);
                            tableModelGenerateResults.addRow(new Object[]{"FATAL", "System", "", errorParts[0]});
                        } else if (result.contains(" ** ")) {
                            int errorCount = 0;
                            int pos = 0;
                            
                            while (pos < result.length()) {
                                int startPattern = result.indexOf(" ** ", pos);
                                if (startPattern == -1) break;
                                
                                int nextPattern = result.indexOf("\n ** ", startPattern + 4);
                                String block = nextPattern != -1 ? 
                                    result.substring(startPattern, nextPattern) : 
                                    result.substring(startPattern);
                                
                                String[] parts = block.split(" \\*\\* ", 4);
                                if (parts.length >= 4) {
                                    String severity = parts[1].trim();
                                    String source = parts[2].trim();
                                    String[] ruleMsg = parts[3].split(" : ", 2);
                                    String ruleId = ruleMsg[0].trim();
                                    String message = ruleMsg.length > 1 ? ruleMsg[1].trim() : "";
                                    tableModelGenerateResults.addRow(new Object[]{severity, source, ruleId, message});
                                    if (!severity.equals("SUCCESS") && !severity.equals("INFO")) {
                                        errorCount++;
                                    }
                                }
                                
                                pos = nextPattern != -1 ? nextPattern + 1 : result.length();
                            }
                            
                            if (errorCount > 0) {
                                statusLabel.setText("⚠ Generated with " + errorCount + " validation issue(s)");
                            } else {
                                statusLabel.setText("✓ Report generated and validated successfully");
                            }
                        } else {
                            statusLabel.setText("✓ Report generated successfully");
                            tableModelGenerateResults.addRow(new Object[]{"SUCCESS", "System", "", "Report generated successfully"});
                        }
                        
                        // Adjust row heights dynamically based on content
                        for (int row = 0; row < tableGenerateResults.getRowCount(); row++) {
                            int maxHeight = 25;
                            for (int col = 0; col < tableGenerateResults.getColumnCount(); col++) {
                                Object value = tableGenerateResults.getValueAt(row, col);
                                if (value != null) {
                                    String text = value.toString();
                                    int columnWidth = tableGenerateResults.getColumnModel().getColumn(col).getWidth();
                                    
                                    // Create a temporary JLabel to calculate wrapped height
                                    JLabel label = new JLabel("<html><div style='width: " + (columnWidth - 10) + "px;'>" 
                                        + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") 
                                        + "</div></html>");
                                    label.setFont(tableGenerateResults.getFont());
                                    Dimension preferredSize = label.getPreferredSize();
                                    maxHeight = Math.max(maxHeight, preferredSize.height + 10);
                                }
                            }
                            tableGenerateResults.setRowHeight(row, maxHeight);
                        }
                    } else {
                        statusLabel.setText("Report generated successfully");
                        infoBox("Report generated successfully", "SUCCESS");
                    }
                } catch (Exception ex) {
                    tableModelGenerateResults.setRowCount(0);
                    tableModelGenerateResults.addRow(new Object[]{"FATAL", "System", "", "Error: " + ex.getMessage()});
                    statusLabel.setText("Error generating report");
                    showError("Report cannot be rendered", ex);
                } finally {
                    bGenerateReport.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }

    private void selectUblInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith(".xml");
            }

            @Override
            public String getDescription() {
                return "XML files (*.xml)";
            }
        });
        
        // Use default input directory if available
        String startDir = defaultInputDir != null ? defaultInputDir : System.getProperty("user.dir");
        chooser.setCurrentDirectory(new File(startDir));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileInput = chooser.getSelectedFile();
            txUblFileName.setText(fileInput.getAbsolutePath());
            statusLabel.setText("Selected: " + fileInput.getName());
        }
    }

    private void validateUbl() {
        if (txUblFileName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select an input file first.",
                "Input Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Template is only required for XML source type
        if (rbSourceXml.isSelected() && cbTemplate.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a template.",
                "Template Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        tableModelUblResults.setRowCount(0);
        statusLabel.setText("Validating UBL document...");
        bValidateUbl.setEnabled(false);
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                File fileInput = new File(txUblFileName.getText());
                
                // Check if source is UBL - validate directly
                if (rbSourceUbl.isSelected()) {
                    // Direct UBL validation without transformation
                    return validateUblDirectly(fileInput);
                }
                
                // Source is XML - need to transform first
                String fileName = FilenameUtils.getBaseName(fileInput.getName());
                String templateName = (String) cbTemplate.getSelectedItem();
                // Extract template name (before " - ")
                if (templateName.contains(" - ")) {
                    templateName = templateName.substring(0, templateName.indexOf(" - "));
                }
                
                // Copy file to input directory for processing
                File file = new File(paramConfig);
                Serializer serializer = new Persister();
                Resources resources = serializer.read(Resources.class, file);
                Resource resource = resources.getResourceByName("global");
                String dirInput = resource.getProperty("dirInput")
                    .replace("%APP_HOME%", resource.getProperty("appHome"))
                    .replace("%PROCESS_HOME%", resource.getProperty("processHome"))
                    .replace("%TEMPLATE%", templateName);
                
                File inputDir = new File(dirInput);
                inputDir.mkdirs();
                File destFile = new File(inputDir, fileName + ".xml");
                FileUtils.copyFile(fileInput, destFile);
                
                // Capture System.out and System.err for validation messages
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.PrintStream ps = new java.io.PrintStream(baos);
                java.io.PrintStream oldOut = System.out;
                java.io.PrintStream oldErr = System.err;
                System.setOut(ps);
                System.setErr(ps);
                
                // Run validation with UBL_VALIDATE mode (validation only, no PA sending)
                try {
                    GenerateReport(templateName, fileName, "UBL_VALIDATE", "1", paramConfig, true);
                    System.out.flush();
                    System.err.flush();
                    System.setOut(oldOut);
                    System.setErr(oldErr);
                    String output = baos.toString();
                    return output;
                } catch (Exception e) {
                    System.setOut(oldOut);
                    System.setErr(oldErr);
                    String output = baos.toString();
                    return "ERROR:" + e.getMessage() + "\n" + output;
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    tableModelUblResults.setRowCount(0);
                    
                    if (result.startsWith("ERROR:")) {
                        statusLabel.setText("✗ Validation failed");
                        String[] errorParts = result.substring(6).split("\n", 2);
                        tableModelUblResults.addRow(new Object[]{"FATAL", "System", "", errorParts[0]});
                        if (errorParts.length > 1) {
                            for (String line : errorParts[1].split("\n")) {
                                if (!line.trim().isEmpty()) {
                                    tableModelUblResults.addRow(new Object[]{"ERROR", "Output", "", line});
                                }
                            }
                        }
                    } else if (result.contains(" ** ")) {
                        // Parse validation error format: " ** SEVERITY ** SOURCE ** RULE_ID : MESSAGE"
                        // Split by the pattern " ** " to handle multi-line messages
                        int errorCount = 0;
                        int pos = 0;
                        
                        while (pos < result.length()) {
                            int startPattern = result.indexOf(" ** ", pos);
                            if (startPattern == -1) break;
                            
                            // Find the next occurrence or end of string
                            int nextPattern = result.indexOf("\n ** ", startPattern + 4);
                            String block = nextPattern != -1 ? 
                                result.substring(startPattern, nextPattern) : 
                                result.substring(startPattern);
                            
                            // Parse this block
                            String[] parts = block.split(" \\*\\* ", 4);
                            if (parts.length >= 4) {
                                // New format with rule ID
                                String severity = parts[1].trim();
                                String source = parts[2].trim();
                                String[] ruleMsg = parts[3].split(" : ", 2);
                                String ruleId = ruleMsg[0].trim();
                                String message = ruleMsg.length > 1 ? ruleMsg[1].trim() : "";
                                tableModelUblResults.addRow(new Object[]{severity, source, ruleId, message});
                                errorCount++;
                            } else if (parts.length >= 3) {
                                // Old format without rule ID (backward compatibility)
                                String severity = parts[1].trim();
                                String[] sourceMsg = parts[2].split(" : ", 2);
                                String source = sourceMsg[0].trim();
                                String message = sourceMsg.length > 1 ? sourceMsg[1].trim() : "";
                                tableModelUblResults.addRow(new Object[]{severity, source, "", message});
                                errorCount++;
                            }
                            
                            pos = nextPattern != -1 ? nextPattern + 1 : result.length();
                        }
                        
                        // Check for success messages
                        if (result.contains("UBL validation successful")) {
                            String[] lines = result.split("\n");
                            for (String line : lines) {
                                if (line.contains("UBL validation successful")) {
                                    tableModelUblResults.addRow(new Object[]{"SUCCESS", "UBL", "", line.trim()});
                                    break;
                                }
                            }
                        }
                        
                        if (errorCount > 0) {
                            statusLabel.setText("⚠ Validation completed with " + errorCount + " issue(s)");
                        } else {
                            statusLabel.setText("✓ Validation successful");
                        }
                        
                        // Adjust row heights dynamically based on content
                        for (int row = 0; row < tableUblResults.getRowCount(); row++) {
                            int maxHeight = 25;
                            for (int col = 0; col < tableUblResults.getColumnCount(); col++) {
                                Object value = tableUblResults.getValueAt(row, col);
                                if (value != null) {
                                    String text = value.toString();
                                    int columnWidth = tableUblResults.getColumnModel().getColumn(col).getWidth();
                                    
                                    // Create a temporary JLabel to calculate wrapped height
                                    JLabel label = new JLabel("<html><div style='width: " + (columnWidth - 10) + "px;'>" 
                                        + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") 
                                        + "</div></html>");
                                    label.setFont(tableUblResults.getFont());
                                    Dimension preferredSize = label.getPreferredSize();
                                    maxHeight = Math.max(maxHeight, preferredSize.height + 10);
                                }
                            }
                            tableUblResults.setRowHeight(row, maxHeight);
                        }
                    } else if (result.isEmpty()) {
                        statusLabel.setText("✓ Validation completed successfully");
                        tableModelUblResults.addRow(new Object[]{"SUCCESS", "UBL", "", "No validation errors found"});
                    } else {
                        // Display raw output
                        for (String line : result.split("\n")) {
                            if (!line.trim().isEmpty()) {
                                tableModelUblResults.addRow(new Object[]{"INFO", "Output", "", line});
                            }
                        }
                        statusLabel.setText("Validation completed");
                    }
                } catch (Exception ex) {
                    tableModelUblResults.setRowCount(0);
                    tableModelUblResults.addRow(new Object[]{"FATAL", "System", "", "Error: " + ex.getMessage()});
                    statusLabel.setText("✗ Validation error");
                    showError("UBL validation error", ex);
                } finally {
                    bValidateUbl.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }

    /**
     * Validates a UBL file directly without transformation
     */
    private String validateUblDirectly(File ublFile) {
        StringBuilder output = new StringBuilder();
        
        try {
            // Get validation configuration from config file
            File file = new File(paramConfig);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);
            Resource resource = resources.getResourceByName("global");
            
            String pAppHome = resource.getProperty("appHome");
            String pProcessHome = resource.getProperty("processHome");
            String pXsdPath = resource.getProperty("ublXsdPath")
                .replace("%APP_HOME%", pAppHome)
                .replace("%PROCESS_HOME%", pProcessHome);
            String pSchematronPath = resource.getProperty("ublSchematronPath")
                .replace("%APP_HOME%", pAppHome)
                .replace("%PROCESS_HOME%", pProcessHome);
            
            // Parse UBL document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document ublDoc = builder.parse(ublFile);
            
            // Validate using UBLValidator
            UBLValidator ublValidator = new UBLValidator(pXsdPath, pSchematronPath);
            ValidationResult validResult = ublValidator.validateUbl(ublDoc);
            
            if (!validResult.isValid()) {
                for (ValidationError e : validResult.getErrors()) {
                    // Format: " ** SEVERITY ** SOURCE ** RULE_ID : MESSAGE"
                    String ruleId = e.getRuleId() != null ? e.getRuleId() : "";
                    output.append(" ** ")
                          .append(e.getSeverity())
                          .append(" ** ")
                          .append(e.getSource())
                          .append(" ** ")
                          .append(ruleId)
                          .append(" : ")
                          .append(e.getMessage())
                          .append("\n");
                }
            } else {
                output.append(" ** SUCCESS ** UBL ** ").append(ublFile.getName())
                      .append(" : UBL validation successful\n");
            }
            
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
        
        return output.toString();
    }

    private void selectDbOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        // Use default input directory if available, or current text field value
        String startDir = !txDbOutputPath.getText().isEmpty() ? txDbOutputPath.getText() :
                         (defaultInputDir != null ? defaultInputDir : System.getProperty("user.dir"));
        chooser.setCurrentDirectory(new File(startDir));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            txDbOutputPath.setText(dir.getAbsolutePath());
            statusLabel.setText("Output directory: " + dir.getName());
        }
    }

    private void selectServerOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        // Use current text field value if set, otherwise default input directory
        String startDir = !txServerOutputPath.getText().isEmpty() ? txServerOutputPath.getText() :
                         (defaultInputDir != null ? defaultInputDir : System.getProperty("user.dir"));
        chooser.setCurrentDirectory(new File(startDir));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            txServerOutputPath.setText(dir.getAbsolutePath());
            statusLabel.setText("Output directory: " + dir.getName());
        }
    }

    private void extractXmlFromDatabase() {
        if (txDbFedoc.getText().trim().isEmpty() || 
            txDbFedct.getText().trim().isEmpty() || 
            txDbKco.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please fill in FEDOC, FEDCT, and FEKCO fields.",
                "Input Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (txDbOutputPath.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select an output directory.",
                "Output Directory Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        txDbResults.setText("");
        statusLabel.setText("Extracting XML from database...");
        bExtractXml.setEnabled(false);
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String fedoc = txDbFedoc.getText().trim();
                String fedct = txDbFedct.getText().trim();
                String kco = txDbKco.getText().trim();
                String outputPath = txDbOutputPath.getText();
                
                // Load database configuration
                File file = new File(paramConfig);
                Serializer serializer = new Persister();
                Resources resources = serializer.read(Resources.class, file);
                Resource resource = resources.getResourceByName("global");
                
                String url = resource.getProperty("URL");
                String dbUser = resource.getProperty("DBUser");
                String dbPassword = decodePasswd(resource.getProperty("DBPassword"));
                String schema = resource.getProperty("schema");
                
                if (url == null || dbUser == null || dbPassword == null) {
                    throw new Exception("Database configuration is missing. Please check URL, DBUser, and DBPassword in config.properties");
                }
                
                // Connect to database
                Class.forName("oracle.jdbc.OracleDriver");
                Connection conn = DriverManager.getConnection(url.trim(), dbUser.trim(), dbPassword);
                
                // Query with schema prefix
                String sql = "SELECT FETXFT, (SELECT MAX(TRIM(FETMPL)) FROM " + schema + ".F564230_LOG WHERE TRIM(F564230_LOG.FEWDS1) = TRIM(F564230.FEWDS1)) FETMPL FROM " + schema + ".F564230 WHERE FEDOC=? AND FEDCT=? AND FEKCO=?";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, fedoc);
                stmt.setString(2, fedct);
                stmt.setString(3, kco);
                
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String fetmpl = rs.getString("FETMPL");
                    Blob blob = rs.getBlob("FETXFT");
                    if (blob != null) {
                        // Read BLOB content
                        byte[] blobBytes = blob.getBytes(1, (int) blob.length());
                        String xmlContent = new String(blobBytes, "UTF-8");
                        
                        // Save to file
                        String fileName = fetmpl + "_" +fedoc + "_" + fedct + "_" + kco + ".xml";
                        File outputFile = new File(outputPath, fileName);
                        FileUtils.writeStringToFile(outputFile, xmlContent, "UTF-8");
                        
                        rs.close();
                        stmt.close();
                        conn.close();
                        
                        return "✓ XML extracted successfully!\n\n" +
                               "Document: FEDOC=" + fedoc + ", FEDCT=" + fedct + ", FEKCO=" + kco + "\n" +
                               "Output file: " + outputFile.getAbsolutePath() + "\n" +
                               "Size: " + (blobBytes.length / 1024) + " KB";
                    } else {
                        rs.close();
                        stmt.close();
                        conn.close();
                        return "✗ No XML data found (FETXFT is null)";
                    }
                } else {
                    rs.close();
                    stmt.close();
                    conn.close();
                    return "✗ No record found with FEDOC=" + fedoc + ", FEDCT=" + fedct + ", FEKCO=" + kco;
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    txDbResults.setText(result);
                    if (result.startsWith("✓")) {
                        statusLabel.setText("Extraction completed successfully");
                    } else {
                        statusLabel.setText("Extraction failed");
                    }
                } catch (Exception ex) {
                    txDbResults.setText("✗ Error during extraction:\n" + ex.getMessage());
                    statusLabel.setText("Extraction error");
                    showError("Database extraction error", ex);
                } finally {
                    bExtractXml.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }

    private void getFile() throws Exception {
        if (tReport.getText().isEmpty() || tVersion.getText().isEmpty() || 
            tLanguage.getText().isEmpty() || tJobNumber.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please fill in all required fields.",
                "Input Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (txServerOutputPath.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select an output directory.",
                "Output Directory Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusLabel.setText("Downloading file from server...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                JSch jsch = new JSch();
                Session session = null;

                File file = new File(paramConfig);
                Serializer serializer = new Persister();
                Resources resources = serializer.read(Resources.class, file);

                Resource resource = resources.getResourceByName("global");
                String pScpDir = resource.getProperty("scpDir");
                String pScpUser = resource.getProperty("scpUser");
                String pScpServer = resource.getProperty("scpServer");
                String pScpPasswd = decodePasswd(resource.getProperty("scpPassword"));
                String outputDir = txServerOutputPath.getText();
                String inputFile = tReport.getText() + "_" + tVersion.getText() + "_" + 
                                 tLanguage.getText() + "_" + tJobNumber.getText() + ".xml";

                session = jsch.getSession(pScpUser, pScpServer, 22);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setPassword(pScpPasswd);
                session.connect();

                Channel channel = session.openChannel("sftp");
                channel.connect();
                ChannelSftp sftpChannel = (ChannelSftp) channel;
                sftpChannel.get(pScpDir + inputFile, outputDir + "/" + inputFile);
                sftpChannel.exit();
                session.disconnect();

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("File downloaded successfully");
                    infoBox("File downloaded successfully", "SUCCESS");
                } catch (Exception ex) {
                    statusLabel.setText("Error downloading file");
                    showError("File couldn't be downloaded", ex);
                }
            }
        };

        worker.execute();
    }

    private String replaceConstValue(String inputStr) {
        String replaceStr = inputStr.replace(APP_HOME, pAppHome);
        replaceStr = replaceStr.replace(PROCESS_HOME, pProcessHome);
        
        // Extract template name (before " - ")
        String template = cbTemplate.getSelectedItem().toString();
        if (template.contains(" - ")) {
            template = template.substring(0, template.indexOf(" - "));
        }
        replaceStr = replaceStr.replace(TEMPLATE, template);
        replaceStr = replaceStr.replace(FILE_NAME, paramFile);
        return replaceStr;
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(this,
            message + "\n" + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    public void MainInit(String pConfig) throws Exception {
        paramConfig = pConfig;
        Serializer serializer = new Persister();
        Template templates = serializer.read(Template.class, new File(paramConfig));

        List<Resource> templateList = templates.getAllTemplates();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        
        for (Resource resource : templateList) {
            String name = resource.getName();
            if (!"global".equals(name)) { // Don't add global to template list
                String description = resource.getProperty("description");
                if (description != null && !description.isEmpty()) {
                    model.addElement(name + " - " + description);
                } else {
                    model.addElement(name);
                }
            }
        }
        
        cbTemplate.setModel(model);
        if (model.getSize() > 0) {
            cbTemplate.setSelectedIndex(0);
        }
        
        // Set default directories from config
        try {
            Resources resources = serializer.read(Resources.class, new File(paramConfig));
            Resource globalResource = resources.getResourceByName("global");
            
            // Get dirInput and remove %TEMPLATE% placeholder
            String dirInput = globalResource.getProperty("dirInput");
            if (dirInput != null) {
                dirInput = dirInput.replace("%APP_HOME%", globalResource.getProperty("appHome"))
                                   .replace("%PROCESS_HOME%", globalResource.getProperty("processHome"))
                                   .replace("%TEMPLATE%/", "")
                                   .replace("%TEMPLATE%", "");
                defaultInputDir = dirInput;
                txDbOutputPath.setText(dirInput);
                txServerOutputPath.setText(dirInput);
            }
        } catch (Exception e) {
            // Ignore if global config not available
            defaultInputDir = System.getProperty("user.dir");
        }
        
        statusLabel.setText("Loaded " + model.getSize() + " templates from " + new File(pConfig).getName());
    }

    public static void main(String[] args) {      
        // Set VS Code dark theme
        DarkTheme.apply();

        SwingUtilities.invokeLater(() -> {
            MainModern frame = new MainModern();
            frame.setVisible(true);
        });
    }
}
