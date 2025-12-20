package custom.ubl;

public class ValidationError {
    private String source;      // XSD, EN16931, CIUS-FR
    private String severity;    // FATAL, ERROR, WARNING
    private String message;
    private String ruleId;
    
    public ValidationError(String source, String severity, String message, String ruleId) {
        this.source = source;
        this.severity = severity;
        this.message = message;
        this.ruleId = ruleId;
    }
    
    // Getters...
    public String getSource() { return source; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getRuleId() { return ruleId; }
}