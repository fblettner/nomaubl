/*
 * Copyright (c) 2025 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * Runtime Log Handler - Manages runtime execution logs
 * Logs are stored in Oracle table according to job execution
 * Different from UBLDatabaseHandler which logs per-document UBL operations
 */
package custom.ubl;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import custom.resources.Resource;
import custom.resources.Resources;
import static custom.resources.Tools.decodePasswd;
import java.io.File;

public class RuntimeLogHandler {

    private final String configFile;
    private final String paramTemplate;
    private final String paramFile;
    private final ProcessingType paramType;
    private final SimpleDateFormat jdeDateFormat = new SimpleDateFormat("yyyyDDD");
    private final SimpleDateFormat jdeTimeFormat = new SimpleDateFormat("HHmmss");
    
    private String dbUrl;
    private String dbSchema;
    private String dbUser;
    private String dbPassword;
    private String dbTableLog;
    private String updateDB;
    
    /**
     * Constructor for RuntimeLogHandler
     * 
     * @param configFile Path to configuration file
     * @param paramTemplate Template name
     * @param paramFile File name being processed
     * @param paramType Processing type (SINGLE, BURST, UBL, BOTH, UBL_VALIDATE)
     */
    public RuntimeLogHandler(String configFile, String paramTemplate, String paramFile, 
                           ProcessingType paramType) {
        this.configFile = configFile;
        this.paramTemplate = paramTemplate;
        this.paramFile = paramFile;
        this.paramType = paramType;
        
        // Load database configuration
        loadDatabaseConfig();
    }
    
    /**
     * Load database configuration from config file
     */
    private void loadDatabaseConfig() {
        try {
            File file = new File(configFile);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);
            
            Resource resource = resources.getResourceByName("global");
            dbUrl = resource.getProperty("URL");
            dbSchema = resource.getProperty("schema");
            dbUser = resource.getProperty("DBUser");
            dbPassword = decodePasswd(resource.getProperty("DBPassword"));
            dbTableLog = resource.getProperty("tableLog") + "_LOG";
            updateDB = resource.getProperty("updateDB");
            
        } catch (Exception e) {
            System.err.println("Failed to load database configuration: " + e.getMessage());
            // Set updateDB to N to disable logging if config fails
            updateDB = "N";
        }
    }
    
    /**
     * Gets current JDE date in CYYDDD format (minus 1900000)
     */
    private int getCurrentJDEDate() {
        Date date = new Date();
        return Integer.parseInt(jdeDateFormat.format(date)) - 1900000;
    }

    /**
     * Gets current JDE time in HHMMSS format (6 digits: 000000-235959)
     */
    private int getCurrentJDETime() {
        Date date = new Date();
        return Integer.parseInt(jdeTimeFormat.format(date));
    }
    
    /**
     * Insert log entry into database
     * 
     * @param method Method or operation being logged
     * @param message Message or status
     * @return LogResult indicating success or failure
     */
    public LogResult insertLog(String method, String message) {
        // Skip if database logging is disabled
        if (!"Y".equalsIgnoreCase(updateDB)) {
            return LogResult.skipped("Database logging disabled");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            // Load JDBC driver
            Class.forName("oracle.jdbc.OracleDriver");
            
            // Connect to database
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            
            // Prepare SQL statement
            String sql = "INSERT INTO " + dbSchema + "." + dbTableLog + 
                        " (FEWDS1, FEUPMJ, FEUPMT, FEMODE, FETMPL, FEMETHOD, FEMESSAGE) " +
                        "VALUES (?,?,?,?,?,?,?)";
            stmt = conn.prepareStatement(sql);
            
            // Set parameters
            stmt.setString(1, paramFile);
            stmt.setInt(2, getCurrentJDEDate());
            stmt.setInt(3, getCurrentJDETime());
            stmt.setString(4, paramType.getValue());
            stmt.setString(5, paramTemplate);
            stmt.setString(6, truncate(method, 100));      // Truncate to avoid DB errors
            stmt.setString(7, truncate(message, 500));     // Truncate to avoid DB errors
            
            // Execute insert
            stmt.executeUpdate();
            
            return LogResult.success();
            
        } catch (ClassNotFoundException e) {
            return LogResult.error("Oracle JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            return LogResult.error("SQL error: " + e.getMessage());
        } catch (Exception e) {
            return LogResult.error("Unexpected error: " + e.getMessage());
        } finally {
            // Clean up resources
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                // Ignore cleanup errors
            }
        }
    }
    
    /**
     * Log START event
     * 
     * @return LogResult indicating success or failure
     */
    public LogResult logStart() {
        return insertLog("START", "SUCCESSFUL");
    }
    
    /**
     * Log END event with status message
     * 
     * @param message Status message (e.g., SUCCESSFUL, FATAL ERROR, NO DATA SELECTED)
     * @return LogResult indicating success or failure
     */
    public LogResult logEnd(String message) {
        return insertLog("END", message);
    }
    
    /**
     * Log an error event
     * 
     * @param method Method where error occurred
     * @param errorMessage Error message
     * @return LogResult indicating success or failure
     */
    public LogResult logError(String method, String errorMessage) {
        return insertLog(method, errorMessage);
    }
    
    /**
     * Truncate string to max length
     * 
     * @param str String to truncate
     * @param maxLength Maximum length
     * @return Truncated string
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Result class for log operations
     */
    public static class LogResult {
        private final boolean success;
        private final boolean skipped;
        private final String errorMessage;
        
        private LogResult(boolean success, boolean skipped, String errorMessage) {
            this.success = success;
            this.skipped = skipped;
            this.errorMessage = errorMessage;
        }
        
        public static LogResult success() {
            return new LogResult(true, false, null);
        }
        
        public static LogResult error(String message) {
            return new LogResult(false, false, message);
        }
        
        public static LogResult skipped(String reason) {
            return new LogResult(true, true, reason);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public boolean isSkipped() {
            return skipped;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean hasError() {
            return !success && !skipped;
        }
    }
}
