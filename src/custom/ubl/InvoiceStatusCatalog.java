package custom.ubl;

/**
 * Centralized catalog for invoice status management
 * Uses UNTDID 1373 standard status codes
 * Provides constants for status codes, lifecycle event messages, and helper methods
 */
public class InvoiceStatusCatalog {

    // ========== UNTDID 1373 STATUS CODES ==========
    public static final String STATUS_APPROVED = "1";              // Approuvée
    public static final String STATUS_REJECTED = "8";              // Rejetée
    public static final String STATUS_DEPOSITED = "10";            // Déposée
    public static final String STATUS_COMPLETED = "37";            // Completée
    public static final String STATUS_SUSPENDED = "39";            // Suspendue
    public static final String STATUS_RECEIVED = "43";             // Reçue
    public static final String STATUS_UNDER_PROCESSING = "45";     // Prise en charge
    public static final String STATUS_DISPUTED = "46";             // En litige
    public static final String STATUS_PAYMENT_PROCESSED = "47";    // Paiement Transmis et Encaissé
    public static final String STATUS_MADE_AVAILABLE = "48";       // Mise à Disposition
    public static final String STATUS_PARTIALLY_APPROVED = "49";   // Approuvée Partiellement
    public static final String STATUS_REFUSED = "50";              // Refusée
    public static final String STATUS_ISSUED = "51";               // Emise
    public static final String STATUS_VALIDATED_WARN = "44";       // Internal: validation with warnings

    // ========== CUSTOM STATUS CODES (for internal workflow) ==========
     public static final String UNDEFINED = "99";                    // Internal: validation with warnings

    // ========== LIFECYCLE EVENT MESSAGES (French - UNTDID 1373) ==========
    public static final String MSG_APPROVED = "Approuvée";
    public static final String MSG_REJECTED = "Rejetée";
    public static final String MSG_DEPOSITED = "Déposée";
    public static final String MSG_COMPLETED = "Completée";
    public static final String MSG_SUSPENDED = "Suspendue";
    public static final String MSG_RECEIVED = "Reçue";
    public static final String MSG_UNDER_PROCESSING = "Prise en charge";
    public static final String MSG_DISPUTED = "En litige";
    public static final String MSG_PAYMENT_PROCESSED = "Paiement Transmis et Encaissé";
    public static final String MSG_MADE_AVAILABLE = "Mise à Disposition";
    public static final String MSG_PARTIALLY_APPROVED = "Approuvée Partiellement";
    public static final String MSG_REFUSED = "Refusée";
    public static final String MSG_ISSUED = "Emise";
    public static final String MSG_VALIDATED_WARN = "Validation avec avertissements";
    
    // Additional context messages
    public static final String MSG_CREATED = "Facture créée";
    public static final String MSG_VALIDATED = "Validation réussie";
    public static final String MSG_SENT = "Envoyée à la PA";
    public static final String MSG_ERROR_SEND = "Échec d'envoi à la PA";
    public static final String MSG_ERROR_VALIDATION = "Échec de validation";

    /**
     * Status transition class to encapsulate status and message together
     */
    public static class StatusTransition {
        private final String status;
        private final String message;

        public StatusTransition(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        /**
         * Apply the status transition to the database handler
         * 
         * @param dbHandler UBL database handler
         * @throws Exception if update or insert fails
         */
        public void apply(UBLDatabaseHandler dbHandler) throws Exception {
            dbHandler.updateInvoiceStatus(status);
            dbHandler.insertLifecycleEvent(status, message);
        }
    }

    // ========== PREDEFINED STATUS TRANSITIONS ==========

    /**
     * Status transition for invoice creation (51 - Emise)
     */
    public static StatusTransition created() {
        return new StatusTransition(STATUS_ISSUED, MSG_CREATED);
    }

    /**
     * Status transition for successful validation (1 - Approuvée)
     */
    public static StatusTransition validated() {
        return new StatusTransition(STATUS_APPROVED, MSG_VALIDATED);
    }

    /**
     * Status transition for validation with warnings (internal code)
     */
    public static StatusTransition validatedWithWarnings() {
        return new StatusTransition(STATUS_VALIDATED_WARN, MSG_VALIDATED_WARN);
    }

    /**
     * Status transition for sending to PA (10 - Déposée)
     */
    public static StatusTransition sent() {
        return new StatusTransition(STATUS_DEPOSITED, MSG_SENT);
    }

    /**
     * Status transition for deposited on PA (10 - Déposée)
     */
    public static StatusTransition deposited() {
        return new StatusTransition(STATUS_DEPOSITED, MSG_DEPOSITED);
    }

    /**
     * Status transition for send error (8 - Rejetée)
     */
    public static StatusTransition errorSend() {
        return new StatusTransition(STATUS_REJECTED, MSG_ERROR_SEND);
    }

    /**
     * Status transition for validation error (8 - Rejetée)
     */
    public static StatusTransition errorValidation() {
        return new StatusTransition(STATUS_REJECTED, MSG_ERROR_VALIDATION);
    }

    /**
     * Status transition for rejection (8 - Rejetée)
     */
    public static StatusTransition rejected() {
        return new StatusTransition(STATUS_REJECTED, MSG_REJECTED);
    }

    /**
     * Status transition for approval (1 - Approuvée)
     */
    public static StatusTransition approved() {
        return new StatusTransition(STATUS_APPROVED, MSG_APPROVED);
    }

    /**
     * Status transition for acceptance (1 - Approuvée)
     * @deprecated Use approved() instead for UNTDID 1373 compliance
     */
    @Deprecated
    public static StatusTransition accepted() {
        return approved();
    }

    /**
     * Status transition for payment processed (47 - Paiement Transmis et Encaissé)
     */
    public static StatusTransition paymentProcessed() {
        return new StatusTransition(STATUS_PAYMENT_PROCESSED, MSG_PAYMENT_PROCESSED);
    }

    /**
     * Status transition for paid
     * @deprecated Use paymentProcessed() instead for UNTDID 1373 compliance
     */
    @Deprecated
    public static StatusTransition paid() {
        return paymentProcessed();
    }

    /**
     * Status transition for received (43 - Reçue)
     */
    public static StatusTransition received() {
        return new StatusTransition(STATUS_RECEIVED, MSG_RECEIVED);
    }

    /**
     * Status transition for under processing (45 - Prise en charge)
     */
    public static StatusTransition underProcessing() {
        return new StatusTransition(STATUS_UNDER_PROCESSING, MSG_UNDER_PROCESSING);
    }

    /**
     * Status transition for disputed (46 - En litige)
     */
    public static StatusTransition disputed() {
        return new StatusTransition(STATUS_DISPUTED, MSG_DISPUTED);
    }

    /**
     * Status transition for made available (48 - Mise à Disposition)
     */
    public static StatusTransition madeAvailable() {
        return new StatusTransition(STATUS_MADE_AVAILABLE, MSG_MADE_AVAILABLE);
    }

    /**
     * Status transition for partially approved (49 - Approuvée Partiellement)
     */
    public static StatusTransition partiallyApproved() {
        return new StatusTransition(STATUS_PARTIALLY_APPROVED, MSG_PARTIALLY_APPROVED);
    }

    /**
     * Status transition for refused (50 - Refusée)
     */
    public static StatusTransition refused() {
        return new StatusTransition(STATUS_REFUSED, MSG_REFUSED);
    }

    /**
     * Status transition for issued (51 - Emise)
     */
    public static StatusTransition issued() {
        return new StatusTransition(STATUS_ISSUED, MSG_ISSUED);
    }

    /**
     * Status transition for completed (37 - Completée)
     */
    public static StatusTransition completed() {
        return new StatusTransition(STATUS_COMPLETED, MSG_COMPLETED);
    }

    /**
     * Status transition for suspended (39 - Suspendue)
     */
    public static StatusTransition suspended() {
        return new StatusTransition(STATUS_SUSPENDED, MSG_SUSPENDED);
    }

    /**
     * Custom status transition
     * 
     * @param status  Status code
     * @param message Lifecycle event message
     */
    public static StatusTransition custom(String status, String message) {
        return new StatusTransition(status, message);
    }
}
