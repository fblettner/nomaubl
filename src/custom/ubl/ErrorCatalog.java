package custom.ubl;

/**
 * Centralized catalog for error management
 * Provides constants for severity, source, and predefined error messages
 */
public class ErrorCatalog {

    // ========== SEVERITY LEVELS ==========
    public static final String SEVERITY_FATAL = "FATAL";
    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_INFO = "INFO";

    // ========== ERROR SOURCES ==========
    public static final String SOURCE_XSD = "XSD";
    public static final String SOURCE_EN16931 = "EN16931";
    public static final String SOURCE_CIUS_FR = "CIUS-FR";
    public static final String SOURCE_UBL = "UBL";
    public static final String SOURCE_PDF = "PDF";
    public static final String SOURCE_XML = "XML";
    public static final String SOURCE_PA = "PA";
    public static final String SOURCE_DB = "DB";
    public static final String SOURCE_TRANSFORM = "TRANSFORM";
    public static final String SOURCE_VALIDATION = "VALIDATION";

    // ========== RULE IDS ==========
    public static final String RULE_PDF_CREATION = "PDF_CREATION";
    public static final String RULE_XML_CREATION = "XML_CREATION";
    public static final String RULE_UBL_CREATION = "UBL_CREATION";
    public static final String RULE_PA_SEND = "PA_SEND";
    public static final String RULE_DB_INSERT = "DB_INSERT";
    public static final String RULE_DB_UPDATE = "DB_UPDATE";
    public static final String RULE_TRANSFORM_XSL = "TRANSFORM_XSL";
    public static final String RULE_TRANSFORM_RTF = "TRANSFORM_RTF";
    public static final String RULE_UBL_VALIDATION = "UBL_VALIDATION";
    public static final String RULE_UBL_ATTACHMENT = "UBL_ATTACHMENT";

    // ========== ERROR MESSAGES ==========
    public static final String MSG_PDF_CREATION_ERROR = "ERREUR CREATION PDF";
    public static final String MSG_XML_CREATION_ERROR = "ERREUR CREATION INDEX";
    public static final String MSG_UBL_CREATION_ERROR = "ERREUR CREATION UBL";
    public static final String MSG_PA_SEND_ERROR = "ERREUR ENVOI PA";
    public static final String MSG_DB_INSERT_ERROR = "ERREUR INSERTION BASE DE DONNEES";
    public static final String MSG_DB_UPDATE_ERROR = "ERREUR MISE A JOUR BASE DE DONNEES";
    public static final String MSG_TRANSFORM_XSL_ERROR = "ERREUR TRANSFORMATION XSL";
    public static final String MSG_TRANSFORM_RTF_ERROR = "ERREUR TRANSFORMATION RTF";
    public static final String MSG_UBL_VALIDATION_ERROR = "ERREUR VALIDATION UBL";
    public static final String MSG_UBL_ATTACHMENT_ERROR = "ERREUR ATTACHEMENT PDF";

    // ========== PREDEFINED ERROR CREATORS ==========

    /**
     * Creates a PDF creation error
     */
    public static ValidationError pdfCreationError() {
        return new ValidationError(SOURCE_PDF, SEVERITY_ERROR, MSG_PDF_CREATION_ERROR, RULE_PDF_CREATION);
    }

    /**
     * Creates a PDF creation error with custom message
     */
    public static ValidationError pdfCreationError(String customMessage) {
        return new ValidationError(SOURCE_PDF, SEVERITY_ERROR, customMessage, RULE_PDF_CREATION);
    }

    /**
     * Creates an XML creation error
     */
    public static ValidationError xmlCreationError() {
        return new ValidationError(SOURCE_XML, SEVERITY_ERROR, MSG_XML_CREATION_ERROR, RULE_XML_CREATION);
    }

    /**
     * Creates an XML creation error with custom message
     */
    public static ValidationError xmlCreationError(String customMessage) {
        return new ValidationError(SOURCE_XML, SEVERITY_ERROR, customMessage, RULE_XML_CREATION);
    }

    /**
     * Creates a UBL creation error
     */
    public static ValidationError ublCreationError() {
        return new ValidationError(SOURCE_UBL, SEVERITY_ERROR, MSG_UBL_CREATION_ERROR, RULE_UBL_CREATION);
    }

    /**
     * Creates a UBL creation error with custom message
     */
    public static ValidationError ublCreationError(String customMessage) {
        return new ValidationError(SOURCE_UBL, SEVERITY_ERROR, customMessage, RULE_UBL_CREATION);
    }

    /**
     * Creates a PA send error
     */
    public static ValidationError paSendError() {
        return new ValidationError(SOURCE_PA, SEVERITY_ERROR, MSG_PA_SEND_ERROR, RULE_PA_SEND);
    }

    /**
     * Creates a PA send error with custom message
     */
    public static ValidationError paSendError(String customMessage) {
        return new ValidationError(SOURCE_PA, SEVERITY_ERROR, customMessage, RULE_PA_SEND);
    }

    /**
     * Creates a database insert error
     */
    public static ValidationError dbInsertError() {
        return new ValidationError(SOURCE_DB, SEVERITY_ERROR, MSG_DB_INSERT_ERROR, RULE_DB_INSERT);
    }

    /**
     * Creates a database insert error with custom message
     */
    public static ValidationError dbInsertError(String customMessage) {
        return new ValidationError(SOURCE_DB, SEVERITY_ERROR, customMessage, RULE_DB_INSERT);
    }

    /**
     * Creates a database update error
     */
    public static ValidationError dbUpdateError() {
        return new ValidationError(SOURCE_DB, SEVERITY_ERROR, MSG_DB_UPDATE_ERROR, RULE_DB_UPDATE);
    }

    /**
     * Creates a database update error with custom message
     */
    public static ValidationError dbUpdateError(String customMessage) {
        return new ValidationError(SOURCE_DB, SEVERITY_ERROR, customMessage, RULE_DB_UPDATE);
    }

    /**
     * Creates a XSL transformation error
     */
    public static ValidationError xslTransformError() {
        return new ValidationError(SOURCE_TRANSFORM, SEVERITY_ERROR, MSG_TRANSFORM_XSL_ERROR, RULE_TRANSFORM_XSL);
    }

    /**
     * Creates a XSL transformation error with custom message
     */
    public static ValidationError xslTransformError(String customMessage) {
        return new ValidationError(SOURCE_TRANSFORM, SEVERITY_ERROR, customMessage, RULE_TRANSFORM_XSL);
    }

    /**
     * Creates a RTF transformation error
     */
    public static ValidationError rtfTransformError() {
        return new ValidationError(SOURCE_TRANSFORM, SEVERITY_ERROR, MSG_TRANSFORM_RTF_ERROR, RULE_TRANSFORM_RTF);
    }

    /**
     * Creates a RTF transformation error with custom message
     */
    public static ValidationError rtfTransformError(String customMessage) {
        return new ValidationError(SOURCE_TRANSFORM, SEVERITY_ERROR, customMessage, RULE_TRANSFORM_RTF);
    }

    /**
     * Creates a UBL validation error
     */
    public static ValidationError ublValidationError() {
        return new ValidationError(SOURCE_VALIDATION, SEVERITY_ERROR, MSG_UBL_VALIDATION_ERROR, RULE_UBL_VALIDATION);
    }

    /**
     * Creates a UBL validation error with custom message
     */
    public static ValidationError ublValidationError(String customMessage) {
        return new ValidationError(SOURCE_VALIDATION, SEVERITY_ERROR, customMessage, RULE_UBL_VALIDATION);
    }

    /**
     * Creates a UBL attachment error
     */
    public static ValidationError ublAttachmentError() {
        return new ValidationError(SOURCE_UBL, SEVERITY_ERROR, MSG_UBL_ATTACHMENT_ERROR, RULE_UBL_ATTACHMENT);
    }

    /**
     * Creates a UBL attachment error with custom message
     */
    public static ValidationError ublAttachmentError(String customMessage) {
        return new ValidationError(SOURCE_UBL, SEVERITY_ERROR, customMessage, RULE_UBL_ATTACHMENT);
    }

    /**
     * Creates a custom error
     * 
     * @param source   Error source
     * @param severity Error severity
     * @param message  Error message
     * @param ruleId   Rule ID
     */
    public static ValidationError customError(String source, String severity, String message, String ruleId) {
        return new ValidationError(source, severity, message, ruleId);
    }

    /**
     * Creates a custom warning
     * 
     * @param source  Error source
     * @param message Error message
     * @param ruleId  Rule ID
     */
    public static ValidationError customWarning(String source, String message, String ruleId) {
        return new ValidationError(source, SEVERITY_WARNING, message, ruleId);
    }

    /**
     * Creates a custom info
     * 
     * @param source  Error source
     * @param message Error message
     * @param ruleId  Rule ID
     */
    public static ValidationError customInfo(String source, String message, String ruleId) {
        return new ValidationError(source, SEVERITY_INFO, message, ruleId);
    }
}
