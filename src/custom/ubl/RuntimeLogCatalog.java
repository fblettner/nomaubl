/*
 * Copyright (c) 2025 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * Runtime Log Catalog - Centralized constants for runtime execution logging
 * Provides method names and status messages for RuntimeLogHandler
 */
package custom.ubl;

public class RuntimeLogCatalog {

    // ========== STATUS MESSAGES ==========
    public static final String STATUS_SUCCESSFUL = "SUCCESSFUL";
    public static final String STATUS_FATAL_ERROR = "FATAL ERROR";
    public static final String STATUS_NO_DATA = "NO DATA SELECTED";
    
    // ========== METHOD NAMES (for tracking where errors occur) ==========
    public static final String METHOD_START = "START";
    public static final String METHOD_END = "END";
    public static final String METHOD_INIT = "Init";
    public static final String METHOD_TRANSFORM_XSL = "transformXSLToXML";
    public static final String METHOD_CONVERT_RTF = "convertRTFXSL";
    public static final String METHOD_RUN_SINGLE = "runSingle";
    public static final String METHOD_RUN_TASKS = "runTasks";
    public static final String METHOD_PARSE_XML = "parseXML";
    public static final String METHOD_COPY_FILES = "copyFiles";
    public static final String METHOD_DELETE_FILES = "deleteFiles";
    public static final String METHOD_PA_AUTH = "paAuthentication";
    
    /**
     * Private constructor to prevent instantiation
     */
    private RuntimeLogCatalog() {
        throw new AssertionError("RuntimeLogCatalog should not be instantiated");
    }
}
