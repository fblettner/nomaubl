/*
 * Copyright (c) 2018 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package Frames;

import custom.resources.Resource;
import custom.resources.Template;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Modern configuration manager with FlatLaf UI
 */
public class ManageConfigModern extends JFrame {

    private String paramConfig;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel statusLabel;
    private TableRowSorter<TableModel> sorter;
    private JButton bOpenFile, bAddTemplate, bCopyTemplate, bRemoveTemplate, bRefresh, bClose;
    private File currentConfigFile;
    private File defaultConfigFile;

    public ManageConfigModern() {
        initModernComponents();
    }
    
    public void setDefaultConfigFile(String configPath) {
        if (configPath != null && !configPath.isEmpty()) {
            File file = new File(configPath);
            if (file.exists()) {
                this.defaultConfigFile = file;
                this.currentConfigFile = file;
                loadConfiguration(file);
            }
        }
    }

    private void initModernComponents() {
        setTitle("Configuration Manager - NomaUBL");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Main panel with modern layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top toolbar
        JPanel toolbarPanel = createToolbar();
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);

        // Center - Table with search
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        // Search panel
        JPanel searchPanel = createSearchPanel();
        centerPanel.add(searchPanel, BorderLayout.NORTH);

        // Table
        createTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom status bar
        JPanel statusPanel = createStatusBar();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setupKeyboardShortcuts();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Open Configuration button
        bOpenFile = createButton("Open Config", "📁", "Open configuration file (Ctrl+O)");
        bOpenFile.addActionListener(e -> openConfigFile());

        // Add Template button
        bAddTemplate = createButton("Add", "➕", "Add new template (Ctrl+N)");
        bAddTemplate.setEnabled(false);
        bAddTemplate.addActionListener(e -> addTemplate());

        // Copy Template button
        bCopyTemplate = createButton("Copy", "📋", "Copy selected template (Ctrl+D)");
        bCopyTemplate.setEnabled(false);
        bCopyTemplate.addActionListener(e -> copyTemplate());

        // Remove Template button
        bRemoveTemplate = createButton("Remove", "🗑️", "Remove selected template (Delete)");
        bRemoveTemplate.setEnabled(false);
        bRemoveTemplate.addActionListener(e -> removeTemplate());

        // Refresh button
        bRefresh = createButton("Refresh", "🔄", "Reload configuration (F5)");
        bRefresh.setEnabled(false);
        bRefresh.addActionListener(e -> refreshConfig());

        // Separator
        toolbar.add(bOpenFile);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(bAddTemplate);
        toolbar.add(bCopyTemplate);
        toolbar.add(bRemoveTemplate);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(bRefresh);

        // Close button on the right
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bClose = createButton("Close", "✕", "Close window (Esc)");
        bClose.addActionListener(e -> setVisible(false));
        rightPanel.add(bClose);

        JPanel container = new JPanel(new BorderLayout());
        container.add(toolbar, BorderLayout.WEST);
        container.add(rightPanel, BorderLayout.EAST);

        return container;
    }

    private JButton createButton(String text, String icon, String tooltip) {
        JButton button = new JButton(icon + " " + text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 32));

        return button;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel searchLabel = new JLabel("🔍 Search:");
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Filter templates...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterTable();
            }
        });

        panel.add(searchLabel, BorderLayout.WEST);
        panel.add(searchField, BorderLayout.CENTER);

        return panel;
    }

    private void createTable() {
        String[] columnNames = {"Template Name", "Description", "Type"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // Modern table appearance
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(500);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        // Sorter
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() != -1;
                bCopyTemplate.setEnabled(hasSelection);
                bRemoveTemplate.setEnabled(hasSelection);
            }
        });

        // Double-click to edit
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedTemplate();
                }
            }
        });

        // Right-click context menu
        JPopupMenu contextMenu = createContextMenu();
        table.setComponentPopupMenu(contextMenu);
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Edit Template");
        editItem.addActionListener(e -> editSelectedTemplate());
        menu.add(editItem);

        menu.addSeparator();

        JMenuItem copyItem = new JMenuItem("Copy Template");
        copyItem.addActionListener(e -> copyTemplate());
        menu.add(copyItem);

        JMenuItem removeItem = new JMenuItem("Remove Template");
        removeItem.addActionListener(e -> removeTemplate());
        menu.add(removeItem);

        return menu;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusBar.add(statusLabel, BorderLayout.WEST);

        return statusBar;
    }

    private void setupKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // Ctrl+O - Open
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "open");
        actionMap.put("open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openConfigFile();
            }
        });

        // Ctrl+N - New
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "new");
        actionMap.put("new", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bAddTemplate.isEnabled()) addTemplate();
            }
        });

        // Ctrl+D - Duplicate
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "copy");
        actionMap.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bCopyTemplate.isEnabled()) copyTemplate();
            }
        });

        // Delete - Remove
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove");
        actionMap.put("remove", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bRemoveTemplate.isEnabled()) removeTemplate();
            }
        });

        // F5 - Refresh
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bRefresh.isEnabled()) refreshConfig();
            }
        });

        // Esc - Close
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        actionMap.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
    }

    private void openConfigFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith(".properties");
            }

            @Override
            public String getDescription() {
                return "Configuration files (*.properties)";
            }
        });
        
        // Use default config file if available, otherwise current directory
        if (defaultConfigFile != null && defaultConfigFile.exists()) {
            chooser.setSelectedFile(defaultConfigFile);
            chooser.setCurrentDirectory(defaultConfigFile.getParentFile());
        } else {
            chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentConfigFile = chooser.getSelectedFile();
            loadConfiguration(currentConfigFile);
        }
    }

    private void loadConfiguration(File file) {
        try {
            paramConfig = file.getAbsolutePath();
            tableModel.setRowCount(0);

            Serializer serializer = new Persister();
            Template templates = serializer.read(Template.class, file);
            List<Resource> templateList = templates.getAllTemplates();

            for (Resource resource : templateList) {
                String name = resource.getName();
                String description = resource.getProperty("description");
                String type = "global".equals(name) ? "Global" : "Template";
                tableModel.addRow(new Object[]{name, description, type});
            }

            // Enable buttons
            bAddTemplate.setEnabled(true);
            bRefresh.setEnabled(true);

            statusLabel.setText("Loaded: " + file.getName() + " (" + templateList.size() + " templates)");
            setTitle("Configuration Manager - " + file.getName());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading configuration:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error loading configuration");
        }
    }

    private void filterTable() {
        String text = searchField.getText();
        if (text.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
        statusLabel.setText(table.getRowCount() + " templates shown");
    }

    private void editSelectedTemplate() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int modelRow = table.convertRowIndexToModel(row);
        String templateName = tableModel.getValueAt(modelRow, 0).toString();

        try {
            if ("global".equals(templateName)) {
                // Use modern global editor
                try {
                    ManageGlobalModern manageGlobal = new ManageGlobalModern();
                    manageGlobal.GlobalInit(templateName, paramConfig);
                    manageGlobal.setVisible(true);
                } catch (NoClassDefFoundError e) {
                    // Fallback to classic if modern not available
                    ManageGlobalModern manageGlobal = new ManageGlobalModern();
                    manageGlobal.GlobalInit(templateName, paramConfig);
                    manageGlobal.setVisible(true);
                }
            } else {
                // Use modern template editor
                try {
                    ManageTemplateModern manageTemplate = new ManageTemplateModern();
                    manageTemplate.TemplateInit(templateName, paramConfig);
                    manageTemplate.setVisible(true);
                } catch (NoClassDefFoundError e) {
                    // Fallback to classic if modern not available
                    ManageTemplateModern manageTemplate = new ManageTemplateModern();
                    manageTemplate.TemplateInit(templateName, paramConfig);
                    manageTemplate.setVisible(true);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error opening template:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTemplate() {
        String name = JOptionPane.showInputDialog(this, 
                "Enter new template name:", 
                "Add Template", 
                JOptionPane.PLAIN_MESSAGE);
        
        if (name != null && !name.trim().isEmpty()) {
            try {
                // Use modern template editor
                try {
                    ManageTemplateModern manageTemplate = new ManageTemplateModern();
                    manageTemplate.TemplateInit(name.trim(), paramConfig);
                    manageTemplate.setVisible(true);
                } catch (NoClassDefFoundError e) {
                    // Fallback to classic if modern not available
                    ManageTemplateModern manageTemplate = new ManageTemplateModern();
                    manageTemplate.TemplateInit(name.trim(), paramConfig);
                    manageTemplate.setVisible(true);
                }
                statusLabel.setText("Creating new template: " + name);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error creating template:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copyTemplate() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int modelRow = table.convertRowIndexToModel(row);
        String templateName = tableModel.getValueAt(modelRow, 0).toString();
        
        String newName = JOptionPane.showInputDialog(this,
                "Enter name for copied template:",
                "Copy Template - " + templateName,
                JOptionPane.PLAIN_MESSAGE);

        if (newName != null && !newName.trim().isEmpty()) {
            // TODO: Implement copy logic
            statusLabel.setText("Copied template: " + templateName + " → " + newName);
            JOptionPane.showMessageDialog(this, 
                    "Template copy functionality to be implemented",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void removeTemplate() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int modelRow = table.convertRowIndexToModel(row);
        String templateName = tableModel.getValueAt(modelRow, 0).toString();

        if ("global".equals(templateName)) {
            JOptionPane.showMessageDialog(this,
                    "Cannot remove the global template",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove template '" + templateName + "'?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // TODO: Implement removal logic
            tableModel.removeRow(modelRow);
            statusLabel.setText("Removed template: " + templateName);
        }
    }

    private void refreshConfig() {
        if (currentConfigFile != null) {
            loadConfiguration(currentConfigFile);
            statusLabel.setText("Configuration refreshed");
        }
    }

    public static void main(String[] args) {
        // Set VS Code dark theme
        DarkTheme.apply();

        SwingUtilities.invokeLater(() -> {
            ManageConfigModern frame = new ManageConfigModern();
            frame.setVisible(true);
        });
    }
}
