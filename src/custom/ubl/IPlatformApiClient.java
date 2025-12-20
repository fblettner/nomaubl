package custom.ubl;

/**
 * Interface for Platform Agréée (PA) API client
 * Allows for easy mocking and testing
 */
public interface IPlatformApiClient {
    
    /**
     * Sends UBL document to the Platform Agréée (PA)
     * 
     * @param ublFilePath Path to the UBL XML file
     * @param docName     Document name for logging
     * @return true if successful, false otherwise
     */
    boolean sendDocument(String ublFilePath, String docName);
    
    /**
     * Checks if API mode is enabled
     * 
     * @return true if API mode is enabled
     */
    boolean isApiMode();
    
    /**
     * Gets the API base URL
     * 
     * @return API base URL
     */
    String getApiBaseUrl();
    
    /**
     * Gets the API import endpoint
     * 
     * @return API import endpoint
     */
    String getApiImportEndpoint();
    
    /**
     * Gets the API timeout
     * 
     * @return API timeout in milliseconds
     */
    int getApiTimeout();
}
