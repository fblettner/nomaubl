/*
 * Copyright (c) 2018 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package Frames;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.UIManager;
import java.awt.Color;

/**
 * VS Code inspired dark theme for NomaUBL modern interfaces
 */
public class DarkTheme {
    
    // VS Code Dark+ color palette
    public static final Color BACKGROUND = new Color(30, 30, 30);           // #1E1E1E - Editor background
    public static final Color PANEL_BACKGROUND = new Color(37, 37, 38);     // #252526 - Side bar background
    public static final Color INPUT_BACKGROUND = new Color(60, 60, 60);     // #3C3C3C - Input background
    public static final Color SELECTION = new Color(38, 79, 120);           // #264F78 - Selection background
    public static final Color BORDER = new Color(69, 69, 69);               // #454545 - Border color
    public static final Color TEXT = new Color(204, 204, 204);              // #CCCCCC - Text color
    public static final Color TEXT_SECONDARY = new Color(150, 150, 150);    // #969696 - Secondary text
    public static final Color ACCENT = new Color(0, 122, 204);              // #007ACC - VS Code blue
    public static final Color BUTTON = new Color(14, 99, 156);              // #0E639C - Button background
    public static final Color BUTTON_HOVER = new Color(28, 151, 234);       // #1C97EA - Button hover
    public static final Color SUCCESS = new Color(73, 190, 170);            // #49BEAA - Success green
    public static final Color ERROR = new Color(244, 71, 71);               // #F44747 - Error red
    public static final Color WARNING = new Color(206, 145, 120);           // #CE9178 - Warning orange
    
    // Validation result table colors (for UBL validation results)
    public static final Color VALIDATION_ERROR_BG = new Color(80, 30, 30);      // Dark red for FATAL/ERROR
    public static final Color VALIDATION_ERROR_FG = new Color(255, 200, 200);   // Light red text
    public static final Color VALIDATION_WARNING_BG = new Color(70, 60, 20);    // Dark yellow for WARNING
    public static final Color VALIDATION_WARNING_FG = new Color(255, 240, 180); // Light yellow text
    public static final Color VALIDATION_SUCCESS_BG = new Color(20, 60, 30);    // Dark green for SUCCESS
    public static final Color VALIDATION_SUCCESS_FG = new Color(180, 255, 200); // Light green text
    
    /**
     * Apply VS Code dark theme to the application
     */
    public static void apply() {
        try {
            // Use FlatLaf Dark as base
            FlatDarkLaf.setup();
            
            // Customize with VS Code colors
            
            // General
            UIManager.put("Panel.background", BACKGROUND);
            UIManager.put("OptionPane.background", BACKGROUND);
            UIManager.put("ScrollPane.background", BACKGROUND);
            
            // Text components
            UIManager.put("TextField.background", INPUT_BACKGROUND);
            UIManager.put("TextField.foreground", TEXT);
            UIManager.put("TextField.caretForeground", TEXT);
            UIManager.put("TextField.selectionBackground", SELECTION);
            UIManager.put("TextField.selectionForeground", Color.WHITE);
            UIManager.put("TextField.disabledBackground", new Color(50, 50, 50));  // Lighter background for disabled
            UIManager.put("TextField.disabledForeground", new Color(220, 220, 220)); // Much lighter text - high contrast
            UIManager.put("TextField.inactiveBackground", new Color(55, 55, 55));
            UIManager.put("TextField.inactiveForeground", new Color(230, 230, 230));
            UIManager.put("TextArea.background", INPUT_BACKGROUND);
            UIManager.put("TextArea.foreground", TEXT);
            UIManager.put("TextArea.selectionBackground", SELECTION);
            UIManager.put("TextArea.selectionForeground", Color.WHITE);
            UIManager.put("TextArea.disabledBackground", new Color(50, 50, 50));
            UIManager.put("TextArea.disabledForeground", new Color(220, 220, 220));
            UIManager.put("TextArea.inactiveBackground", new Color(55, 55, 55));
            UIManager.put("TextArea.inactiveForeground", new Color(230, 230, 230));
            UIManager.put("TextComponent.arc", 0);
            
            // Buttons - Modern rounded style
            UIManager.put("Button.background", BUTTON);
            UIManager.put("Button.foreground", TEXT);
            UIManager.put("Button.hoverBackground", BUTTON_HOVER);
            UIManager.put("Button.pressedBackground", ACCENT);
            UIManager.put("Button.arc", 20);  // Subtle rounded corners
            UIManager.put("Button.focusWidth", 0);  // Remove focus border
            UIManager.put("Button.innerFocusWidth", 0);
            UIManager.put("Button.borderWidth", 0);  // Remove border for cleaner look
            UIManager.put("Button.selectedBackground", ACCENT);
            UIManager.put("Button.borderWidth", 1);
            
            // Tables
            UIManager.put("Table.background", BACKGROUND);
            UIManager.put("Table.foreground", TEXT);
            UIManager.put("Table.selectionBackground", SELECTION);
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Table.gridColor", BORDER);
            UIManager.put("TableHeader.background", PANEL_BACKGROUND);
            UIManager.put("TableHeader.foreground", TEXT);
            
            // Tabs
            UIManager.put("TabbedPane.background", BACKGROUND);
            UIManager.put("TabbedPane.foreground", TEXT);
            UIManager.put("TabbedPane.selectedBackground", PANEL_BACKGROUND);
            UIManager.put("TabbedPane.selectedForeground", TEXT);
            UIManager.put("TabbedPane.underlineColor", ACCENT);
            UIManager.put("TabbedPane.hoverColor", PANEL_BACKGROUND);
            
            // ComboBox
            UIManager.put("ComboBox.background", INPUT_BACKGROUND);
            UIManager.put("ComboBox.foreground", TEXT);
            UIManager.put("ComboBox.selectionBackground", SELECTION);
            UIManager.put("ComboBox.selectionForeground", Color.WHITE);
            UIManager.put("ComboBox.buttonBackground", PANEL_BACKGROUND);
            UIManager.put("ComboBox.arc", 0);
            
            // Lists
            UIManager.put("List.background", BACKGROUND);
            UIManager.put("List.foreground", TEXT);
            UIManager.put("List.selectionBackground", SELECTION);
            UIManager.put("List.selectionForeground", Color.WHITE);
            
            // Progress bar
            UIManager.put("ProgressBar.background", PANEL_BACKGROUND);
            UIManager.put("ProgressBar.foreground", ACCENT);
            UIManager.put("ProgressBar.selectionBackground", TEXT);
            UIManager.put("ProgressBar.selectionForeground", TEXT);
            UIManager.put("ProgressBar.arc", 0);
            
            // Menu
            UIManager.put("Menu.background", PANEL_BACKGROUND);
            UIManager.put("Menu.foreground", TEXT);
            UIManager.put("MenuItem.background", PANEL_BACKGROUND);
            UIManager.put("MenuItem.foreground", TEXT);
            UIManager.put("MenuItem.selectionBackground", SELECTION);
            UIManager.put("MenuItem.selectionForeground", Color.WHITE);
            UIManager.put("PopupMenu.background", PANEL_BACKGROUND);
            UIManager.put("PopupMenu.foreground", TEXT);
            
            // Labels
            UIManager.put("Label.foreground", TEXT);
            UIManager.put("Label.disabledForeground", TEXT_SECONDARY);
            
            // Borders and separators
            UIManager.put("Component.borderColor", BORDER);
            UIManager.put("Component.focusedBorderColor", ACCENT);
            UIManager.put("Separator.foreground", BORDER);
            
            // Titled border
            UIManager.put("TitledBorder.titleColor", TEXT);
            
            // Scroll bars
            UIManager.put("ScrollBar.track", BACKGROUND);
            UIManager.put("ScrollBar.thumb", BORDER);
            UIManager.put("ScrollBar.thumbHighlight", TEXT_SECONDARY);
            UIManager.put("ScrollBar.thumbDarkShadow", BORDER);
            UIManager.put("ScrollBar.width", 10);
            
            // Tooltips
            UIManager.put("ToolTip.background", PANEL_BACKGROUND);
            UIManager.put("ToolTip.foreground", TEXT);
            UIManager.put("ToolTip.border", new javax.swing.border.LineBorder(BORDER, 1));
            
            // Status bar
            UIManager.put("StatusBar.background", PANEL_BACKGROUND);
            
            // Tree
            UIManager.put("Tree.background", BACKGROUND);
            UIManager.put("Tree.foreground", TEXT);
            UIManager.put("Tree.selectionBackground", SELECTION);
            UIManager.put("Tree.selectionForeground", Color.WHITE);
            
            // CheckBox and RadioButton
            UIManager.put("CheckBox.background", BACKGROUND);
            UIManager.put("CheckBox.foreground", TEXT);
            UIManager.put("RadioButton.background", BACKGROUND);
            UIManager.put("RadioButton.foreground", TEXT);
            
            // Window
            UIManager.put("window", BACKGROUND);
            
        } catch (Exception ex) {
            System.err.println("Failed to apply dark theme: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Get a color with adjusted alpha
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
