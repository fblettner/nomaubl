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
 * Modern template configuration editor with organized tabs
 */
public class ManageTemplateModern extends JFrame {

    private String template;
    private String paramConfig;
    private boolean isNewTemplate = false;
    
    // UI Components
    private JTextField tName, tDescription;
    private JTextField tID, tActivite, tTypePiece, tTypeJDE, tSociete;
    private JTextField tNumClient, tMontant, tDatePiece, tDateEcheance;
    private JTextField tBurstKey, tTransformYN, tTransform, tXSL, tRTF;
    private JTextField tCodeRoutage, tCPU;
    private JButton bSave, bCancel;
    private JLabel statusLabel;
    private JTabbedPane tabbedPane;
    private boolean modified = false;

    public ManageTemplateModern() {
        initModernComponents();
    }

    private void initModernComponents() {
        setTitle("Template Configuration - NomaUBL");
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
        tabbedPane.addTab("📄 Document", createDocumentPanel());
        tabbedPane.addTab("🔧 Processing", createProcessingPanel());
        tabbedPane.addTab("⚙️ Advanced", createAdvancedPanel());
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

        // Template name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblName = new JLabel("Template:");
        lblName.setFont(lblName.getFont().deriveFont(Font.BOLD));
        panel.add(lblName, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        tName = new JTextField();
        tName.setFont(tName.getFont().deriveFont(Font.BOLD, 14f));
        tName.setToolTipText("Unique template identifier");
        panel.add(tName, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1;
        tDescription = new JTextField();
        tDescription.setToolTipText("Brief description of this template");
        panel.add(tDescription, gbc);

        return panel;
    }

    private JPanel createDocumentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Document identification
        JPanel docIdPanel = new JPanel(new GridBagLayout());
        docIdPanel.setBorder(createTitledBorder("Document Identification"));
        GridBagConstraints docGbc = new GridBagConstraints();
        docGbc.insets = new Insets(5, 5, 5, 5);
        docGbc.fill = GridBagConstraints.HORIZONTAL;
        docGbc.weightx = 1.0;

        addFieldToPanel(docIdPanel, docGbc, 0, "Activity:", 
                       tActivite = createTextField("Business activity code"));
        addFieldToPanel(docIdPanel, docGbc, 1, "Type:", 
                       tTypePiece = createTextField("Type of document (invoice, order, etc.)"));
        addFieldToPanel(docIdPanel, docGbc, 2, "Document ID:", 
                       tID = createTextField("XML document identifier (XPath)"));
        addFieldToPanel(docIdPanel, docGbc, 3, "Document Type:", 
                       tTypeJDE = createTextField("Document Type"));
        addFieldToPanel(docIdPanel, docGbc, 4, "Document Company:", 
                       tSociete = createTextField("Document Company"));

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        fieldsPanel.add(docIdPanel, gbc);

        // Document data
        JPanel dataPanel = new JPanel(new GridBagLayout());
        dataPanel.setBorder(createTitledBorder("Document Data (XPath)"));
        GridBagConstraints dataGbc = new GridBagConstraints();
        dataGbc.insets = new Insets(5, 5, 5, 5);
        dataGbc.fill = GridBagConstraints.HORIZONTAL;
        dataGbc.weightx = 1.0;

        addFieldToPanel(dataPanel, dataGbc, 0, "Customer Number:", 
                       tNumClient = createTextField("XPath to customer number"));
        addFieldToPanel(dataPanel, dataGbc, 1, "Amount:", 
                       tMontant = createTextField("XPath to document amount"));
        addFieldToPanel(dataPanel, dataGbc, 2, "Document Date:", 
                       tDatePiece = createTextField("XPath to document date"));
        addFieldToPanel(dataPanel, dataGbc, 3, "Due Date:", 
                       tDateEcheance = createTextField("XPath to due date"));

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        fieldsPanel.add(dataPanel, gbc);

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
        gbc.weightx = 1.0;

        int row = 0;

        // Transformation
        JPanel transformPanel = new JPanel(new GridBagLayout());
        transformPanel.setBorder(createTitledBorder("XSL Transformation"));
        GridBagConstraints transGbc = new GridBagConstraints();
        transGbc.insets = new Insets(5, 5, 5, 5);
        transGbc.fill = GridBagConstraints.HORIZONTAL;

        transGbc.gridx = 0; transGbc.gridy = 0; transGbc.weightx = 0;
        JLabel lblTransform = new JLabel("Transform:");
        lblTransform.setPreferredSize(new Dimension(120, lblTransform.getPreferredSize().height));
        transformPanel.add(lblTransform, transGbc);
        transGbc.gridx = 1; transGbc.weightx = 0.2;
        tTransformYN = new JTextField(5);
        tTransformYN.setToolTipText("Enable transformation (Y/N)");
        transformPanel.add(tTransformYN, transGbc);

        transGbc.gridx = 0; transGbc.gridy = 1; transGbc.weightx = 0;
        JLabel lblTransformXSL = new JLabel("Transform XSL:");
        lblTransformXSL.setPreferredSize(new Dimension(120, lblTransformXSL.getPreferredSize().height));
        transformPanel.add(lblTransformXSL, transGbc);
        transGbc.gridx = 1; transGbc.weightx = 1.0; transGbc.gridwidth = 2;
        tTransform = createTextField("Path to transformation XSL file");
        transformPanel.add(tTransform, transGbc);
        transGbc.gridwidth = 1;

        transGbc.gridx = 0; transGbc.gridy = 2; transGbc.weightx = 0;
        JLabel lblMainXSL = new JLabel("Main XSL:");
        lblMainXSL.setPreferredSize(new Dimension(120, lblMainXSL.getPreferredSize().height));
        transformPanel.add(lblMainXSL, transGbc);
        transGbc.gridx = 1; transGbc.weightx = 1.0; transGbc.gridwidth = 2;
        tXSL = createTextField("Path to main XSL template");
        transformPanel.add(tXSL, transGbc);
        transGbc.gridwidth = 1;

        transGbc.gridx = 0; transGbc.gridy = 3; transGbc.weightx = 0;
        JLabel lblRTF = new JLabel("RTF Template:");
        lblRTF.setPreferredSize(new Dimension(120, lblRTF.getPreferredSize().height));
        transformPanel.add(lblRTF, transGbc);
        transGbc.gridx = 1; transGbc.weightx = 1.0; transGbc.gridwidth = 2;
        tRTF = createTextField("Path to RTF template file");
        transformPanel.add(tRTF, transGbc);
        transGbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        fieldsPanel.add(transformPanel, gbc);

        // Bursting
        JPanel burstPanel = new JPanel(new GridBagLayout());
        burstPanel.setBorder(createTitledBorder("Bursting Configuration"));
        GridBagConstraints burstGbc = new GridBagConstraints();
        burstGbc.insets = new Insets(5, 5, 5, 5);
        burstGbc.fill = GridBagConstraints.HORIZONTAL;

        burstGbc.gridx = 0; burstGbc.gridy = 0; burstGbc.weightx = 0;
        JLabel lblBurstKey = new JLabel("Burst Key:");
        lblBurstKey.setPreferredSize(new Dimension(120, lblBurstKey.getPreferredSize().height));
        burstPanel.add(lblBurstKey, burstGbc);
        burstGbc.gridx = 1; burstGbc.weightx = 1.0;
        tBurstKey = createTextField("XPath expression for burst key");
        burstPanel.add(tBurstKey, burstGbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        fieldsPanel.add(burstPanel, gbc);

        // Routing
        JPanel routingPanel = new JPanel(new GridBagLayout());
        routingPanel.setBorder(createTitledBorder("Routing"));
        GridBagConstraints routGbc = new GridBagConstraints();
        routGbc.insets = new Insets(5, 5, 5, 5);
        routGbc.fill = GridBagConstraints.HORIZONTAL;

        routGbc.gridx = 0; routGbc.gridy = 0; routGbc.weightx = 0;
        JLabel lblRoutingCode = new JLabel("Routing Code:");
        lblRoutingCode.setPreferredSize(new Dimension(120, lblRoutingCode.getPreferredSize().height));
        routingPanel.add(lblRoutingCode, routGbc);
        routGbc.gridx = 1; routGbc.weightx = 1.0;
        tCodeRoutage = createTextField("Code for document routing");
        routingPanel.add(tCodeRoutage, routGbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        fieldsPanel.add(routingPanel, gbc);

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAdvancedPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Performance
        JPanel perfPanel = new JPanel(new GridBagLayout());
        perfPanel.setBorder(createTitledBorder("Performance Settings"));
        GridBagConstraints perfGbc = new GridBagConstraints();
        perfGbc.insets = new Insets(5, 5, 5, 5);
        perfGbc.fill = GridBagConstraints.HORIZONTAL;

        perfGbc.gridx = 0; perfGbc.gridy = 0; perfGbc.weightx = 0;
        JLabel lblCPU = new JLabel("Number of CPUs:");
        lblCPU.setPreferredSize(new Dimension(120, lblCPU.getPreferredSize().height));
        perfPanel.add(lblCPU, perfGbc);
        perfGbc.gridx = 1; perfGbc.weightx = 1.0;
        tCPU = new JTextField(10);
        tCPU.setToolTipText("Number of processors for parallel processing (1-N)");
        perfPanel.add(tCPU, perfGbc);

        JLabel cpuHelp = new JLabel("💡 For parallel processing in burst mode");
        cpuHelp.setFont(cpuHelp.getFont().deriveFont(Font.ITALIC, 11f));
        cpuHelp.setForeground(Color.GRAY);
        perfGbc.gridx = 0; perfGbc.gridy = 1; perfGbc.gridwidth = 2;
        perfPanel.add(cpuHelp, perfGbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        fieldsPanel.add(perfPanel, gbc);

        // Help section
        JPanel helpPanel = new JPanel(new BorderLayout(10, 10));
        helpPanel.setBorder(createTitledBorder("Configuration Tips"));
        
        JTextArea helpText = new JTextArea();
        helpText.setEditable(false);
        helpText.setWrapStyleWord(true);
        helpText.setLineWrap(true);
        helpText.setText(
            "XPath Expressions:\n" +
            "Use XPath to locate data in XML documents. Examples:\n" +
            "  • /Invoice/ID - Simple path\n" +
            "  • //Customer/@Number - Attribute\n" +
            "  • /Invoice/LineItems/Line[1]/Amount - Indexed\n\n" +
            "File Paths:\n" +
            "Use absolute or relative paths. Available variables:\n" +
            "  • %APP_HOME% - Application root\n" +
            "  • %PROCESS_HOME% - Processing directory\n" +
            "  • %TEMPLATE% - Template name\n\n" +
            "CPU Settings:\n" +
            "  • 1 = Single processor (default)\n" +
            "  • 2+ = Parallel processing (burst mode only)\n" +
            "  • Higher numbers = faster processing (if enough cores available)"
        );
        helpText.setRows(12);
        JScrollPane helpScroll = new JScrollPane(helpText);
        helpPanel.add(helpScroll, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1; gbc.weightx = 1.0;
        fieldsPanel.add(helpPanel, gbc);

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
        bSave.setToolTipText("Save template configuration (Ctrl+S)");
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

    private void addFieldToPanel(JPanel panel, GridBagConstraints gbc, int row, 
                                 String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(120, lbl.getPreferredSize().height));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
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
        Component[] components = {tName, tDescription, tID, tActivite, tTypePiece, tTypeJDE, 
            tSociete, tNumClient, tMontant, tDatePiece, tDateEcheance, tBurstKey, 
            tTransformYN, tTransform, tXSL, tRTF, tCodeRoutage, tCPU};
        
        for (Component comp : components) {
            if (comp instanceof JTextField) {
                ((JTextField) comp).getDocument().addDocumentListener(docListener);
            }
        }
    }

    private void setModified() {
        if (!modified) {
            modified = true;
            String title = getTitle();
            if (!title.startsWith("* ")) {
                setTitle("* " + title);
            }
            statusLabel.setText("Modified - remember to save");
        }
    }

    private void saveConfiguration() {
        try {
            // Validate template name
            String templateName = tName.getText().trim();
            if (templateName.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Template name cannot be empty!",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                tabbedPane.setSelectedIndex(0);
                tName.requestFocus();
                return;
            }

            File file = new File(paramConfig);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);
            
            // For new templates or name change, update the template name
            if (isNewTemplate || !templateName.equals(template)) {
                // Check if new name already exists
                if (!templateName.equals(template)) {
                    try {
                        Resource existing = resources.getResourceByName(templateName);
                        if (existing != null) {
                            JOptionPane.showMessageDialog(this,
                                "A template with this name already exists!",
                                "Duplicate Name",
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } catch (Exception e) {
                        // Template doesn't exist, which is what we want
                    }
                }
                template = templateName;
            }
            
            Resource resource = resources.getResourceByName(template);
            
            // Save all fields
            resource.setProperty("description", tDescription.getText());
            resource.setProperty("docID", tID.getText());
            resource.setProperty("activite", tActivite.getText());
            resource.setProperty("typePiece", tTypePiece.getText());
            resource.setProperty("typeJDE", tTypeJDE.getText());
            resource.setProperty("societeJDE", tSociete.getText());
            resource.setProperty("numClient", tNumClient.getText());
            resource.setProperty("montant", tMontant.getText());
            resource.setProperty("datePiece", tDatePiece.getText());
            resource.setProperty("dateEcheance", tDateEcheance.getText());
            resource.setProperty("burstKey", tBurstKey.getText());
            resource.setProperty("transformYN", tTransformYN.getText());
            resource.setProperty("transform", tTransform.getText());
            resource.setProperty("xsl", tXSL.getText());
            resource.setProperty("rtf", tRTF.getText());
            resource.setProperty("numProc", tCPU.getText());
            resource.setProperty("codeRoutage", tCodeRoutage.getText());

            serializer.write(resources, file);

            modified = false;
            setTitle(getTitle().replace("* ", ""));
            statusLabel.setText("Template saved successfully");
            
            JOptionPane.showMessageDialog(this,
                "Template '" + template + "' saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);

            dispose();

        } catch (Exception ex) {
            statusLabel.setText("Error saving template");
            JOptionPane.showMessageDialog(this,
                "Error saving template:\n" + ex.getMessage(),
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

    public void TemplateInit(String pTemplate, String pConfig) throws Exception {
        template = pTemplate;
        paramConfig = pConfig;

        File file = new File(paramConfig);
        Serializer serializer = new Persister();
        Resources resources = serializer.read(Resources.class, file);
        
        try {
            Resource resource = resources.getResourceByName(template);
            
            // Existing template - load all fields
            tName.setText(template);
            tName.setEditable(false);
            
            tDescription.setText(resource.getProperty("description"));
            tID.setText(resource.getProperty("docID"));
            tActivite.setText(resource.getProperty("activite"));
            tTypePiece.setText(resource.getProperty("typePiece"));
            tTypeJDE.setText(resource.getProperty("typeJDE"));
            tSociete.setText(resource.getProperty("societeJDE"));
            tNumClient.setText(resource.getProperty("numClient"));
            tMontant.setText(resource.getProperty("montant"));
            tDatePiece.setText(resource.getProperty("datePiece"));
            tDateEcheance.setText(resource.getProperty("dateEcheance"));
            tBurstKey.setText(resource.getProperty("burstKey"));
            tTransformYN.setText(resource.getProperty("transformYN"));
            tTransform.setText(resource.getProperty("transform"));
            tXSL.setText(resource.getProperty("xsl"));
            tRTF.setText(resource.getProperty("rtf"));
            tCPU.setText(resource.getProperty("numProc"));
            tCodeRoutage.setText(resource.getProperty("codeRoutage"));

            isNewTemplate = false;
            statusLabel.setText("Template loaded");
            
        } catch (Exception e) {
            // New template
            tName.setText(template);
            tName.setEditable(true);
            
            // Set defaults
            tTransformYN.setText("Y");
            tCPU.setText("1");
            
            isNewTemplate = true;
            statusLabel.setText("New template - configure and save");
        }

        // Reset modified flag after loading
        modified = false;
    }

    public static void main(String[] args) {
        // Set VS Code dark theme
        DarkTheme.apply();

        SwingUtilities.invokeLater(() -> {
            ManageTemplateModern frame = new ManageTemplateModern();
            frame.setVisible(true);
        });
    }
}
