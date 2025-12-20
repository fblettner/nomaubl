/*
 * Copyright (c) 2025 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * JDE User Updater - Updates user information in JDE database tables
 * Handles updates to F986110 table for job tracking
 */
package custom.ubl;

import java.sql.*;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import custom.resources.Resource;
import custom.resources.Resources;
import static custom.resources.Tools.decodePasswd;
import java.io.File;

public class JDEUserUpdater {

    private final String configFile;
    private String dbUrl;
    private String dbSchemaSVM;
    private String dbUser;
    private String dbPassword;
    
    /**
     * Constructor for JDEUserUpdater
     * 
     * @param configFile Path to configuration file containing database connection details
     */
    public JDEUserUpdater(String configFile) {
        this.configFile = configFile;
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
            dbSchemaSVM = resource.getProperty("schemaSVM");
            dbUser = resource.getProperty("DBUser");
            dbPassword = decodePasswd(resource.getProperty("DBPassword"));
            
        } catch (Exception e) {
            System.err.println("Failed to load JDE database configuration: " + e.getMessage());
        }
    }
    
    /**
     * Update user to 'EXPLOIT' for specified job number and job name
     * Updates the JCUSER field in F986110 table
     * 
     * @param jobNumber Job number to update
     * @param jobName Job name (used to extract additional job number from filename)
     * @return UpdateResult indicating success or failure
     */
    public UpdateResult updateUser(String jobNumber, String jobName) {
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        
        try {
            // Load Oracle JDBC driver
            Class.forName("oracle.jdbc.OracleDriver");
            
            // Connect to database
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            
            // First update: Update by job number parameter
            String sql = "UPDATE " + dbSchemaSVM + ".F986110 SET JCUSER='EXPLOIT' WHERE JCJOBNBR=?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(jobNumber));
            int rowsUpdated1 = stmt.executeUpdate();
            
            // Second update: Extract job number from job name and update
            String sql2 = "UPDATE " + dbSchemaSVM + ".F986110 SET JCUSER='EXPLOIT' WHERE JCJOBNBR=?";
            stmt2 = conn.prepareStatement(sql2);
            String[] parts = jobName.split("_");
            if (parts.length >= 3) {
                stmt2.setInt(1, Integer.parseInt(parts[2]));
                int rowsUpdated2 = stmt2.executeUpdate();
                return UpdateResult.success(rowsUpdated1 + rowsUpdated2);
            } else {
                return UpdateResult.success(rowsUpdated1);
            }
            
        } catch (ClassNotFoundException e) {
            return UpdateResult.error("Oracle JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            return UpdateResult.error("SQL error during user update: " + e.getMessage());
        } catch (NumberFormatException e) {
            return UpdateResult.error("Invalid job number format: " + e.getMessage());
        } catch (Exception e) {
            return UpdateResult.error("Unexpected error during user update: " + e.getMessage());
        } finally {
            // Clean up resources
            try {
                if (stmt != null) stmt.close();
                if (stmt2 != null) stmt2.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                // Ignore cleanup errors
            }
        }
    }
    
    /**
     * Result class for update operations
     */
    public static class UpdateResult {
        private final boolean success;
        private final String errorMessage;
        private final int rowsUpdated;
        
        private UpdateResult(boolean success, String errorMessage, int rowsUpdated) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.rowsUpdated = rowsUpdated;
        }
        
        public static UpdateResult success(int rowsUpdated) {
            return new UpdateResult(true, null, rowsUpdated);
        }
        
        public static UpdateResult error(String message) {
            return new UpdateResult(false, message, 0);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public int getRowsUpdated() {
            return rowsUpdated;
        }
        
        public boolean hasError() {
            return !success;
        }
    }
}
