package custom.ubl;

import java.io.File;

/**
 * Mock implementation of Platform Agréée (PA) API client for testing
 * Simulates PA responses without actual API calls
 */
public class MockPlatformApiClient implements IPlatformApiClient {
    
    private final String mode;
    private final String apiBaseUrl;
    private final String apiImportEndpoint;
    private final int apiTimeout;
    private final boolean displayError;
    private final MockBehavior behavior;
    
    /**
     * Mock behavior configuration
     */
    public enum MockBehavior {
        /** Always succeed */
        ALWAYS_SUCCESS,
        /** Always fail */
        ALWAYS_FAILED,
        /** Alternate: odd calls succeed, even calls fail */
        ALTERNATING,
        /** Random success/failure (70% success rate) */
        RANDOM
    }
    
    private int callCount = 0;
    
    /**
     * Creates a mock PA API client with default success behavior
     * 
     * @param mode              API mode ("API" or other)
     * @param apiBaseUrl        Base URL for the PA API
     * @param apiImportEndpoint Import endpoint path
     * @param apiTimeout        Timeout in milliseconds
     * @param displayError      Whether to display errors
     */
    public MockPlatformApiClient(String mode, String apiBaseUrl, String apiImportEndpoint, 
                                int apiTimeout, boolean displayError) {
        this(mode, apiBaseUrl, apiImportEndpoint, apiTimeout, displayError, MockBehavior.ALWAYS_SUCCESS);
    }
    
    /**
     * Creates a mock PA API client with specified behavior
     * 
     * @param mode              API mode ("API" or other)
     * @param apiBaseUrl        Base URL for the PA API
     * @param apiImportEndpoint Import endpoint path
     * @param apiTimeout        Timeout in milliseconds
     * @param displayError      Whether to display errors
     * @param behavior          Mock behavior
     */
    public MockPlatformApiClient(String mode, String apiBaseUrl, String apiImportEndpoint, 
                                int apiTimeout, boolean displayError, MockBehavior behavior) {
        this.mode = mode;
        this.apiBaseUrl = apiBaseUrl;
        this.apiImportEndpoint = apiImportEndpoint;
        this.apiTimeout = apiTimeout;
        this.displayError = displayError;
        this.behavior = behavior;
    }
    
    @Override
    public boolean sendDocument(String ublFilePath, String docName) {
        if (!"API".equalsIgnoreCase(mode)) {
            log(LogCatalog.info(LogCatalog.MODULE_PA, LogCatalog.SUB_PA_MODE, 
                "[MOCK] Not in API mode, skipping send for " + docName));
            return true;
        }
        
        // Verify file exists
        File ublFile = new File(ublFilePath);
        if (!ublFile.exists()) {
            log(LogCatalog.error(LogCatalog.MODULE_PA, LogCatalog.SUB_PA_SEND, 
                "[MOCK] File not found: " + ublFilePath));
            return false;
        }
        
        callCount++;
        
        boolean success = determineSuccess();
        
        if (success) {
            log(LogCatalog.success(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, 
                "[MOCK] Document sent successfully: " + docName + " (call #" + callCount + ")"));
            log(LogCatalog.info(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, 
                "[MOCK] Response: {\"status\":\"success\",\"documentId\":\"MOCK-" + System.currentTimeMillis() + "\"}"));
        } else {
            log(LogCatalog.error(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, 
                "[MOCK] Failed to send document " + docName + " - Status: 500 (call #" + callCount + ")"));
            log(LogCatalog.error(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, 
                "[MOCK] Response: {\"error\":\"Internal Server Error\",\"message\":\"Simulated error\"}"));
        }
        
        return success;
    }
    
    /**
     * Determines if the mock call should succeed based on behavior
     */
    private boolean determineSuccess() {
        switch (behavior) {
            case ALWAYS_SUCCESS:
                return true;
                
            case ALWAYS_FAILED:
                return false;
                
            case ALTERNATING:
                return callCount % 2 == 1;
                                
            case RANDOM:
                return Math.random() > 0.3; // 70% success rate
                
            default:
                return true;
        }
    }
    
    @Override
    public boolean isApiMode() {
        return "API".equalsIgnoreCase(mode);
    }
    
    @Override
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    @Override
    public String getApiImportEndpoint() {
        return apiImportEndpoint;
    }
    
    @Override
    public int getApiTimeout() {
        return apiTimeout;
    }
    
    /**
     * Log using a LogEntry from LogCatalog
     * 
     * @param entry LogEntry to log
     */
    private void log(LogCatalog.LogEntry entry) {
        entry.print(displayError);
    }
    
    /**
     * Resets the call counter (useful for testing)
     */
    public void resetCallCount() {
        callCount = 0;
    }
    
    /**
     * Gets the number of calls made to sendDocument
     * 
     * @return call count
     */
    public int getCallCount() {
        return callCount;
    }
}
