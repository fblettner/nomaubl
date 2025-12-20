package custom.ubl;

/**
 * Mock implementation of TokenManager for testing authentication flows
 * Simulates token generation and expiration without connecting to PA API
 */
public class MockTokenManager extends TokenManager {
    
    private int tokenRequestCount = 0;
    private final TokenBehavior behavior;
    private final boolean displayError;
    
    /**
     * Token simulation behaviors
     */
    public enum TokenBehavior {
        /** Always return valid token */
        ALWAYS_SUCCESS,
        /** Always fail to get token */
        ALWAYS_FAILED,
        /** First 2 attempts fail (simulate expiration), then succeed */
        INVALID_TOKEN,
        /** Random success/failure */
        RANDOM
    }
    
    /**
     * Creates a mock token manager with ALWAYS_SUCCESS behavior
     */
    public MockTokenManager(String baseUrl, String loginEndpoint, String username, String password, int timeout, boolean displayError) {
        this(baseUrl, loginEndpoint, username, password, timeout, TokenBehavior.ALWAYS_SUCCESS, displayError);
    }
    
    /**
     * Creates a mock token manager with specified behavior
     */
    public MockTokenManager(String baseUrl, String loginEndpoint, String username, String password, int timeout, TokenBehavior behavior, boolean displayError) {
        super(baseUrl, loginEndpoint, username, password, timeout);
        this.behavior = behavior;
        this.displayError = displayError;
        LogCatalog.mockTokenInitialized(behavior.toString()).print(displayError);
    }
    
    @Override
    public String getToken() {
        tokenRequestCount++;
        
        boolean success = determineSuccess();
        
        if (success) {
            String mockToken = "MOCK_TOKEN_" + System.currentTimeMillis();
            LogCatalog.mockTokenSuccess(tokenRequestCount, mockToken.substring(0, 20) + "...").print(displayError);
            return mockToken;
        } else {
            LogCatalog.mockTokenFailed(tokenRequestCount).print(displayError);
            return null;
        }
    }
    
    /**
     * Determines if token request should succeed based on behavior
     */
    private boolean determineSuccess() {
        switch (behavior) {
            case ALWAYS_SUCCESS:
                return true;
                
            case ALWAYS_FAILED:
                return true;
                
            case INVALID_TOKEN:
                // First 2 requests fail (simulating expired token + refresh issues), then succeed
                if (tokenRequestCount <= 2) {
                    LogCatalog.mockTokenExpiring(tokenRequestCount, 2).print(displayError);
                    return false;
                }
                LogCatalog.mockTokenRefreshed().print(displayError);
                return true;
                
            case RANDOM:
                return Math.random() > 0.3; // 70% success rate
                
            default:
                return true;
        }
    }
    
    /**
     * Resets the token request counter
     */
    public void resetRequestCount() {
        tokenRequestCount = 0;
    }
    
    /**
     * Gets the number of token requests made
     */
    public int getRequestCount() {
        return tokenRequestCount;
    }
}
