/*
 * Copyright (c) 2025 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package custom.ubl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Manages authentication token for PA API with automatic refresh
 */
public class TokenManager {
    
    private final String baseUrl;
    private final String loginEndpoint;
    private final String username;
    private final String password;
    private final int timeout;
    private final boolean displayError;
    
    private String currentToken;
    private long tokenExpiryTime;
    
    public TokenManager(String baseUrl, String loginEndpoint, String username, String password, int timeout) {
        this(baseUrl, loginEndpoint, username, password, timeout, false);
    }
    
    public TokenManager(String baseUrl, String loginEndpoint, String username, String password, int timeout, boolean displayError) {
        this.baseUrl = baseUrl;
        this.loginEndpoint = loginEndpoint;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.displayError = displayError;
        this.currentToken = null;
        this.tokenExpiryTime = 0;
    }
    
    /**
     * Gets a valid token, refreshing if necessary
     * @return JWT token or null if authentication failed
     */
    public synchronized String getToken() {
        // Check if we need a new token (expired or doesn't exist)
        // Assume token valid for 55 minutes (to refresh before actual expiry at 60min)
        long currentTime = System.currentTimeMillis();
        if (currentToken == null || currentTime >= tokenExpiryTime) {
            return refreshToken();
        }
        return currentToken;
    }
    
    /**
     * Forces a token refresh (useful when 401 Unauthorized is received)
     * @return new JWT token or null if authentication failed
     */
    public synchronized String refreshToken() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeout))
                    .build();

            String jsonPayload = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + loginEndpoint))
                    .timeout(Duration.ofMillis(timeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                // Parse JSON response to extract token
                // Expected format: {"token":"JWT_TOKEN_HERE"}
                int tokenStart = responseBody.indexOf("\"token\":\"") + 9;
                int tokenEnd = responseBody.indexOf("\"", tokenStart);
                if (tokenStart > 8 && tokenEnd > tokenStart) {
                    currentToken = responseBody.substring(tokenStart, tokenEnd);
                    // Set expiry time to 55 minutes from now
                    tokenExpiryTime = System.currentTimeMillis() + (55 * 60 * 1000);
                    LogCatalog.tokenAuthSuccess().print(displayError);
                    return currentToken;
                }
            }
            
            LogCatalog.tokenAuthFailed(response.statusCode(), response.body()).print(displayError);
            currentToken = null;
            return null;

        } catch (Exception e) {
            LogCatalog.tokenAuthException(e.getMessage()).print(displayError);
            currentToken = null;
            return null;
        }
    }
    
    /**
     * Invalidates the current token to force refresh on next request
     */
    public synchronized void invalidateToken() {
        currentToken = null;
        tokenExpiryTime = 0;
    }
}
