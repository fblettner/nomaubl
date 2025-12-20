package custom.ubl;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Client for Platform Agréée (PA) API communication
 * Handles authentication, document sending, and error handling
 */
public class PlatformApiClient implements IPlatformApiClient {

    private final String mode;
    private final String apiBaseUrl;
    private final String apiImportEndpoint;
    private final int apiTimeout;
    private final TokenManager tokenManager;
    private final boolean displayError;

    /**
     * Creates a new PA API client
     * 
     * @param mode              API mode ("API" or other)
     * @param apiBaseUrl        Base URL for the PA API
     * @param apiImportEndpoint Import endpoint path
     * @param apiTimeout        Timeout in milliseconds
     * @param tokenManager      Token manager for authentication
     * @param displayError      Whether to display errors
     */
    public PlatformApiClient(String mode, String apiBaseUrl, String apiImportEndpoint, 
                            int apiTimeout, TokenManager tokenManager, boolean displayError) {
        this.mode = mode;
        this.apiBaseUrl = apiBaseUrl;
        this.apiImportEndpoint = apiImportEndpoint;
        this.apiTimeout = apiTimeout;
        this.tokenManager = tokenManager;
        this.displayError = displayError;
    }

    /**
     * Sends UBL file to the Platform Agréée (PA) via API
     * 
     * @param ublFilePath Path to the UBL XML file
     * @param docName     Document name for logging
     * @return true if successful, false otherwise
     */
    @Override
    public boolean sendDocument(String ublFilePath, String docName) {
        if (!"API".equalsIgnoreCase(mode)) {
            log(LogCatalog.paNotApi(docName));
            return true;
        }

        if (tokenManager == null) {
            log(LogCatalog.paTokenManagerNotInitialized(docName));
            return false;
        }

        try {
            // Read UBL file and encode to base64
            File ublFile = new File(ublFilePath);
            byte[] ublBytes = new byte[(int) ublFile.length()];
            try (FileInputStream fis = new FileInputStream(ublFile)) {
                fis.read(ublBytes);
            }
            String base64Ubl = Base64.getEncoder().encodeToString(ublBytes);

            // Try sending with current token, retry once with refreshed token if 401
            for (int attempt = 0; attempt < 2; attempt++) {
                String token = tokenManager.getToken();
                if (token == null) {
                    log(LogCatalog.paAuthFailed(docName));
                    return false;
                }

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(apiTimeout))
                        .build();

                String jsonPayload = String.format(
                        "{\"format\":\"xml_ubl\",\"content\":\"%s\",\"postActions\":[]}",
                        base64Ubl);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiBaseUrl + apiImportEndpoint))
                        .timeout(Duration.ofMillis(apiTimeout))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log(LogCatalog.paDocumentSent(docName));
                    log(LogCatalog.info(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, "Response: " + response.body()));
                    return true;
                } else if (response.statusCode() == 401 && attempt == 0) {
                    // Token expired, refresh and retry
                    log(LogCatalog.paTokenExpired(docName));
                    tokenManager.refreshToken();
                    continue;
                } else {
                    log(LogCatalog.paSendError(docName, response.statusCode()));
                    log(LogCatalog.error(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, "Response: " + response.body()));
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            log(LogCatalog.paSendException(docName, e.getMessage()));
            e.printStackTrace();
            return false;
        }
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
     * Checks if API mode is enabled
     * 
     * @return true if API mode is enabled
     */
    @Override
    public boolean isApiMode() {
        return "API".equalsIgnoreCase(mode);
    }

    /**
     * Gets the API base URL
     * 
     * @return API base URL
     */
    @Override
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    /**
     * Gets the API import endpoint
     * 
     * @return API import endpoint
     */
    @Override
    public String getApiImportEndpoint() {
        return apiImportEndpoint;
    }

    /**
     * Gets the API timeout
     * 
     * @return API timeout in milliseconds
     */
    @Override
    public int getApiTimeout() {
        return apiTimeout;
    }
}
