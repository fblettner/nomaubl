package custom.ubl;

/**
 * Mock implementation of TokenManager for testing authentication flows
 * Simulates token generation and expiration without connecting to PA API
 */
public class MockTokenManager extends TokenManager {
    
    private int tokenRequestCount = 0;
    private final TokenBehavior behavior;
    
    /**
     * Token simulation behaviors
     */
    public enum TokenBehavior {
        /** Always return valid token */
        ALWAYS_SUCCESS,
        /** Always fail to get token */
        ALWAYS_FAIL,
        /** First 2 attempts fail (simulate expiration), then succeed */
        INVALID_TOKEN,
        /** Random success/failure */
        RANDOM
    }
    
    /**
     * Creates a mock token manager with ALWAYS_SUCCESS behavior
     */
    public MockTokenManager(String baseUrl, String loginEndpoint, String username, String password, int timeout) {
        this(baseUrl, loginEndpoint, username, password, timeout, TokenBehavior.ALWAYS_SUCCESS);
    }
    
    /**
     * Creates a mock token manager with specified behavior
     */
    public MockTokenManager(String baseUrl, String loginEndpoint, String username, String password, int timeout, TokenBehavior behavior) {
        super(baseUrl, loginEndpoint, username, password, timeout);
        this.behavior = behavior;
        System.out.println("** INFO ** MOCK ** TOKEN : MockTokenManager initialized with behavior: " + behavior);
    }
    
    @Override
    public String getToken() {
        tokenRequestCount++;
        
        boolean success = determineSuccess();
        
        if (success) {
            String mockToken = "MOCK_TOKEN_" + System.currentTimeMillis();
            System.out.println("** SUCCESS ** MOCK ** TOKEN : Token generated successfully (request #" + tokenRequestCount + "): " + mockToken.substring(0, 20) + "...");
            return mockToken;
        } else {
            System.out.println("** ERROR ** MOCK ** TOKEN : Failed to generate token (request #" + tokenRequestCount + ") - Simulating authentication failure");
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
                
            case ALWAYS_FAIL:
                return true;
                
            case INVALID_TOKEN:
                // First 2 requests fail (simulating expired token + refresh issues), then succeed
                if (tokenRequestCount <= 2) {
                    System.out.println("** WARNING ** MOCK ** TOKEN : Simulating token expiration scenario (attempt " + tokenRequestCount + "/2)");
                    return false;
                }
                System.out.println("** INFO ** MOCK ** TOKEN : Token refresh successful after expiration");
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
