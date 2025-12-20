/*
 * Copyright (c) 2025 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * Processing Type Enum - Defines the types of document processing modes
 */
package custom.ubl;

public enum ProcessingType {
    /**
     * Process a single document - generates PDF only
     */
    SINGLE("SINGLE"),
    
    /**
     * Process multiple documents in batch - generates PDFs only
     */
    BURST("BURST"),
    
    /**
     * Generate UBL XML files only (no PDF generation)
     */
    UBL("UBL"),
    
    /**
     * Generate both PDF and UBL XML files
     */
    BOTH("BOTH"),
    
    /**
     * Validate UBL files without sending to Platform API
     */
    UBL_VALIDATE("UBL_VALIDATE");
    
    private final String value;
    
    ProcessingType(String value) {
        this.value = value;
    }
    
    /**
     * Get the string value of the processing type
     * @return String representation
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Parse string to ProcessingType enum
     * @param value String value to parse
     * @return ProcessingType enum value
     * @throws IllegalArgumentException if value is not valid
     */
    public static ProcessingType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Processing type cannot be null");
        }
        
        for (ProcessingType type : ProcessingType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException(
            "Invalid processing type: " + value + 
            ". Valid types are: SINGLE, BURST, UBL, BOTH, UBL_VALIDATE"
        );
    }
    
    /**
     * Check if this type involves UBL processing
     * @return true if UBL, BOTH, or UBL_VALIDATE
     */
    public boolean involvesUBL() {
        return this == UBL || this == BOTH || this == UBL_VALIDATE;
    }
    
    /**
     * Check if this type involves PDF generation
     * @return true if SINGLE, BURST, or BOTH
     */
    public boolean involvesPDF() {
        return this == SINGLE || this == BURST || this == BOTH;
    }
    
    /**
     * Check if this is a batch processing type
     * @return true if BURST, UBL, BOTH, or UBL_VALIDATE
     */
    public boolean isBatch() {
        return this != SINGLE;
    }
    
    /**
     * Check if this type should send to Platform API
     * @return true if UBL or BOTH (not UBL_VALIDATE)
     */
    public boolean shouldSendToPA() {
        return this == UBL || this == BOTH;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
