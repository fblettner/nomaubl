package custom.ubl;

/**
 * Centralized catalog for logging management
 * Provides constants for log levels, modules, and standardized logging
 */
public class LogCatalog {

    // ========== LOG LEVELS ==========
    public static final String LEVEL_INFO = "INFO";
    public static final String LEVEL_SUCCESS = "SUCCESS";
    public static final String LEVEL_WARNING = "WARNING";
    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_DEBUG = "DEBUG";

    // ========== MODULES ==========
    public static final String MODULE_UBL = "UBL";
    public static final String MODULE_PDF = "PDF";
    public static final String MODULE_XML = "XML";
    public static final String MODULE_PA = "PA";
    public static final String MODULE_DB = "DB";
    public static final String MODULE_TRANSFORM = "TRANSFORM";
    public static final String MODULE_VALIDATION = "VALIDATION";
    public static final String MODULE_SYSTEM = "SYSTEM";

    // ========== SUBMODULES ==========
    // PA submodules
    public static final String SUB_PA_MODE = "Mode";
    public static final String SUB_PA_AUTH = "Auth";
    public static final String SUB_PA_SEND = "Send";
    public static final String SUB_PA_TOKEN = "TokenManager";

    // DB submodules
    public static final String SUB_DB_INSERT = "INSERT";
    public static final String SUB_DB_UPDATE = "UPDATE";
    public static final String SUB_DB_STATUS = "Status";
    public static final String SUB_DB_UBL_TABLES = "UBL Tables";
    public static final String SUB_DB_CONNECTION = "Connection";

    // UBL submodules
    public static final String SUB_UBL_CREATION = "Creation";
    public static final String SUB_UBL_VALIDATION = "Validation";
    public static final String SUB_UBL_ATTACHMENT = "Attachment";
    public static final String SUB_UBL_PA = "PA";

    // Generic submodules
    public static final String SUB_PROCESSING = "Processing";
    public static final String SUB_INITIALIZATION = "Initialization";
    public static final String SUB_CONFIGURATION = "Configuration";

    /**
     * Log entry class to build structured log messages
     */
    public static class LogEntry {
        private final String level;
        private final String module;
        private final String submodule;
        private final String message;

        private LogEntry(String level, String module, String submodule, String message) {
            this.level = level;
            this.module = module;
            this.submodule = submodule;
            this.message = message;
        }

        public String getLevel() {
            return level;
        }

        public String getModule() {
            return module;
        }

        public String getSubmodule() {
            return submodule;
        }

        public String getMessage() {
            return message;
        }

        /**
         * Formats the log entry as: ** LEVEL ** MODULE ** SUBMODULE : message
         */
        public String format() {
            return String.format(" ** %s ** %s ** %s : %s",
                    level.toUpperCase(),
                    module.toUpperCase(),
                    submodule,
                    message);
        }

        /**
         * Outputs the log entry to console
         * Errors go to stderr, everything else to stdout
         */
        public void print(boolean displayError) {
            if (displayError) {
                if (LEVEL_ERROR.equalsIgnoreCase(level)) {
                    System.err.println(format());
                } else {
                    System.out.println(format());
                }
            }
        }

        /**
         * Builder for creating log entries
         */
        public static class Builder {
            private String level;
            private String module;
            private String submodule;
            private String message;

            public Builder level(String level) {
                this.level = level;
                return this;
            }

            public Builder module(String module) {
                this.module = module;
                return this;
            }

            public Builder submodule(String submodule) {
                this.submodule = submodule;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public LogEntry build() {
                return new LogEntry(level, module, submodule, message);
            }
        }

        public static Builder builder() {
            return new Builder();
        }
    }

    // ========== CONVENIENCE METHODS FOR COMMON LOG PATTERNS ==========

    /**
     * PA related logs
     */
    public static LogEntry paNotApi(String docName) {
        return LogEntry.builder()
                .level(LEVEL_INFO)
                .module(MODULE_PA)
                .submodule(SUB_PA_MODE)
                .message("not API, skipping send for " + docName)
                .build();
    }

    public static LogEntry paTokenManagerNotInitialized(String docName) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_PA)
                .submodule(SUB_PA_TOKEN)
                .message("not initialized for " + docName)
                .build();
    }

    public static LogEntry paAuthFailed(String docName) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_PA)
                .submodule(SUB_PA_AUTH)
                .message("Failed to get auth token for " + docName)
                .build();
    }

    public static LogEntry paDocumentSent(String docName) {
        return LogEntry.builder()
                .level(LEVEL_SUCCESS)
                .module(MODULE_UBL)
                .submodule(SUB_UBL_PA)
                .message("Document sent successfully: " + docName)
                .build();
    }

    public static LogEntry paTokenExpired(String docName) {
        return LogEntry.builder()
                .level(LEVEL_WARNING)
                .module(MODULE_UBL)
                .submodule(SUB_UBL_PA)
                .message("Token expired, refreshing and retrying for " + docName)
                .build();
    }

    public static LogEntry paSendError(String docName, int statusCode) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_UBL)
                .submodule(SUB_UBL_PA)
                .message("Failed to send document " + docName + " (HTTP " + statusCode + ")")
                .build();
    }

    public static LogEntry paSendException(String docName, String error) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_UBL)
                .submodule(SUB_UBL_PA)
                .message("Exception sending document " + docName + ": " + error)
                .build();
    }

    /**
     * UBL related logs
     */
    public static LogEntry ublAttachmentError(String docName) {
        return LogEntry.builder()
                .level(LEVEL_WARNING)
                .module(MODULE_UBL)
                .submodule(SUB_UBL_ATTACHMENT)
                .message("Could not embed PDF attachment in UBL for " + docName)
                .build();
    }

    public static LogEntry ublAttachmentSuccess(String docName) {
        return LogEntry.builder()
                .level(LEVEL_SUCCESS)
                .module(MODULE_UBL)
                .submodule(SUB_UBL_ATTACHMENT)
                .message("PDF attachment embedded in UBL for " + docName)
                .build();
    }

    public static LogEntry ublValidationSuccess(String typePiece, String docName) {
        return LogEntry.builder()
                .level(LEVEL_SUCCESS)
                .module(MODULE_UBL)
                .submodule(typePiece)
                .message("validation successful for " + docName)
                .build();
    }

    public static LogEntry ublForceSendToPA(String docName) {
        return LogEntry.builder()
                .level(LEVEL_INFO)
                .module(MODULE_UBL)
                .submodule(SUB_UBL_PA)
                .message("forcing send to PA despite warnings for " + docName)
                .build();
    }

    /**
     * Database related logs
     */
    public static LogEntry dbInsertFailed(String error) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_DB)
                .submodule(SUB_DB_INSERT)
                .message("Insert failed: " + error)
                .build();
    }

    public static LogEntry dbUpdateFailed(String error) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_DB)
                .submodule(SUB_DB_STATUS)
                .message("Update failed: " + error)
                .build();
    }

    public static LogEntry dbUblTablesFailed(String docName) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_DB)
                .submodule(SUB_DB_UBL_TABLES)
                .message("Failed to populate UBL tables for " + docName)
                .build();
    }

    public static LogEntry dbUblTablesError(String error) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(MODULE_DB)
                .submodule(SUB_DB_UBL_TABLES)
                .message(error)
                .build();
    }

    /**
     * Generic log creators
     */
    public static LogEntry generic(String level, String module, String submodule, String message) {
        return LogEntry.builder()
                .level(level)
                .module(module)
                .submodule(submodule)
                .message(message)
                .build();
    }

    public static LogEntry info(String module, String submodule, String message) {
        return LogEntry.builder()
                .level(LEVEL_INFO)
                .module(module)
                .submodule(submodule)
                .message(message)
                .build();
    }

    public static LogEntry success(String module, String submodule, String message) {
        return LogEntry.builder()
                .level(LEVEL_SUCCESS)
                .module(module)
                .submodule(submodule)
                .message(message)
                .build();
    }

    public static LogEntry warning(String module, String submodule, String message) {
        return LogEntry.builder()
                .level(LEVEL_WARNING)
                .module(module)
                .submodule(submodule)
                .message(message)
                .build();
    }

    public static LogEntry error(String module, String submodule, String message) {
        return LogEntry.builder()
                .level(LEVEL_ERROR)
                .module(module)
                .submodule(submodule)
                .message(message)
                .build();
    }

    public static LogEntry debug(String module, String submodule, String message) {
        return LogEntry.builder()
                .level(LEVEL_DEBUG)
                .module(module)
                .submodule(submodule)
                .message(message)
                .build();
    }
}
