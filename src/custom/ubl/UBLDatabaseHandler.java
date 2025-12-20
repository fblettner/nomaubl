/*
 * Copyright (c) 2025 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * UBL Database Handler - Populates Oracle tables according to specification STD010301
 * Tables: F564231 (Header), F564233 (Lines), F564234 (VAT Summary), F564235 (Lifecycle), F564236 (Validation)
 */
package custom.ubl;

import org.w3c.dom.*;
import javax.xml.xpath.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class UBLDatabaseHandler {

    private final Connection conn;
    private final String schema;
    private final String doc;
    private final String dct;
    private final String kco;
    private final SimpleDateFormat jdeFormat = new SimpleDateFormat("yyyyDDD");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
    private final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");
    private final XPath xpath;
    private final Properties statusCodeMapping;
    private final boolean displayError;

    // Constructor
    public UBLDatabaseHandler(Connection connection, String schema, String doc, String dct, String kco, String ublConfigPath, boolean displayError) throws XPathExpressionException {
        this.conn = connection;
        this.schema = schema;
        this.doc = doc;
        this.dct = dct;
        this.kco = kco;
        this.displayError = displayError;
        XPathFactory xpathFactory = XPathFactory.newInstance();
        this.xpath = xpathFactory.newXPath();
        
        // Load mapping files
        this.statusCodeMapping = loadPropertiesFile(ublConfigPath + "ubl_status.properties", "STATUS");

        // Register UBL namespaces
        xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if ("cbc".equals(prefix)) return "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
                if ("cac".equals(prefix)) return "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
                if ("inv".equals(prefix)) return "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
                return null;
            }
            @Override
            public String getPrefix(String namespaceURI) { return null; }
            @Override
            public java.util.Iterator<String> getPrefixes(String namespaceURI) { return null; }
        });
    }
    
    /**
     * Generic method to load properties file from filesystem or classpath
     * 
     * @param path Path to properties file (absolute or classpath)
     * @param mappingName Name of the mapping for logging purposes
     * @return Loaded Properties object (empty if file not found)
     */
    private Properties loadPropertiesFile(String path, String mappingName) {
        Properties props = new Properties();
        
        if (path == null || path.trim().isEmpty()) {
            System.out.println(" ** WARNING ** DB ** " + mappingName + " : No path provided, using default codes");
            return props;
        }
        
        try {
            InputStream is = null;
            File file = new File(path);
            if (file.exists()) {
                is = new FileInputStream(file);
            }
            
            if (is != null) {
                try {
                    props.load(is);
                    System.out.println(" ** INFO ** DB ** " + mappingName + " : Code mapping loaded: " + props.size() + " codes");
                } finally {
                    is.close();
                }
            } else {
                System.out.println(" ** WARNING ** DB ** " + mappingName + " : File not found at: " + path);
            }
        } catch (IOException e) {
            System.out.println(" ** WARNING ** DB ** " + mappingName + " : Failed to load mapping: " + e.getMessage());
        }
        
        return props;
    }
    
    /**
     * Generic method to map a code using a properties mapping
     * 
     * @param mapping Properties object containing the mapping
     * @param code Input code to map
     * @param maxLength Maximum length for JDE field (will truncate if needed)
     * @param mappingName Name of the mapping for logging purposes
     * @param defaultValue Default value if no mapping found
     * @return Mapped code (or default if not found)
     */
    private String mapCode(Properties mapping, String code, int maxLength, String mappingName, String defaultValue) {
        if (code == null) return null;
        
        String mappedCode = mapping.getProperty(code);
        if (mappedCode != null) {
            // Ensure it fits in the maximum length
            if (mappedCode.length() > maxLength) {
                mappedCode = mappedCode.substring(0, maxLength);
            }
            return mappedCode;
        } else {
            // No mapping found, return default or original code
            if (defaultValue != null) {
                return defaultValue;
            } else {
                // Return original code, truncated if necessary
                if (code.length() > maxLength) {
                    return code.substring(0, maxLength);
                }
                return code;
            }
        }
    }
    
    /**
     * Maps a descriptive status code to JDE UDC code (max 10 characters)
     * Uses ubl_status_codes.properties mapping file
     * 
     * @param statusCode Descriptive status code (e.g., "VALIDATED_WARN")
     * @return JDE-compliant code (e.g., "VALID_WARN") or "99" if not found
     */
    private String mapStatusCode(String statusCode) {
        return mapCode(statusCodeMapping, statusCode, 10, "STATUS", "99");
    }
    
    /**
     * Converts ISO date (yyyy-MM-dd) to JDE Julian format (CYYDDD - 1900000)
     */
    private int convertToJDEDate(String isoDate) throws Exception {
        if (isoDate == null || isoDate.isEmpty()) return 0;
        Date date = dateParser.parse(isoDate);
        return Integer.parseInt(jdeFormat.format(date)) - 1900000;
    }

    /**
     * Gets current JDE date
     */
    private int getCurrentJDEDate() {
        Date date = new Date();
        return Integer.parseInt(jdeFormat.format(date)) - 1900000;
    }

    /**
     * Gets current JDE time in HHMMSS format (6 digits: 000000-235959)
     * Example: 143052 = 14:30:52, 093015 = 09:30:15
     */
    private int getCurrentJDETime() {
        Date date = new Date();
        return Integer.parseInt(timeFormat.format(date));
    }

    /**
     * Extract text content from XPath
     */
    private String getXPathValue(Document doc, String expression) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(expression);
        String value = (String) expr.evaluate(doc, XPathConstants.STRING);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    /**
     * Extract numeric content from XPath
     */
    private Double getXPathNumeric(Document doc, String expression) throws XPathExpressionException {
        String value = getXPathValue(doc, expression);
        if (value == null) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Scale a numeric value with precision (for JDE numeric fields)
     * Uses BigDecimal to avoid floating-point precision issues
     * 
     * @param value String value to scale
     * @param scaleFactor Scale factor (e.g., 10000 for 4 decimals, 100 for 2 decimals)
     * @return Scaled value as double, or 0 if value is null
     */
    private double scaleNumeric(String value, int scaleFactor) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            BigDecimal decimal = new BigDecimal(value);
            BigDecimal scaled = decimal.multiply(BigDecimal.valueOf(scaleFactor));
            // Round to 2 decimal places to avoid floating-point artifacts
            return scaled.setScale(2, RoundingMode.HALF_UP).doubleValue();
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Set a String parameter in PreparedStatement with null handling
     * If value is null, inserts blank string instead
     * 
     * @param stmt PreparedStatement to set parameter on
     * @param paramIndex Parameter index (1-based)
     * @param value String value (can be null)
     */
    private void setStringOrBlank(PreparedStatement stmt, int paramIndex, String value) throws SQLException {
        stmt.setString(paramIndex, value != null ? value : " ");
    }

    /**
     * Set an Integer parameter in PreparedStatement with null handling
     * If value is null, inserts 0 instead
     * 
     * @param stmt PreparedStatement to set parameter on
     * @param paramIndex Parameter index (1-based)
     * @param value Integer value as String (can be null)
     */
    private void setIntegerOrZero(PreparedStatement stmt, int paramIndex, String value) throws SQLException {
        if (value != null && !value.trim().isEmpty()) {
            try {
                stmt.setInt(paramIndex, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                stmt.setInt(paramIndex, 0);
            }
        } else {
            stmt.setInt(paramIndex, 0);
        }
    }

    /**
     * Set a Double parameter in PreparedStatement with null handling
     * If value is null, inserts 0.0 instead
     * 
     * @param stmt PreparedStatement to set parameter on
     * @param paramIndex Parameter index (1-based)
     * @param value Double value (can be null)
     */
    private void setDoubleOrZero(PreparedStatement stmt, int paramIndex, Double value) throws SQLException {
        stmt.setDouble(paramIndex, value != null ? value : 0.0);
    }

    /**
     * Convert Document to CLOB for storing XML
     */
    private Blob convertNodeToBlob(Document doc) throws Exception {
        javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = tf.newTransformer();
        javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
        java.io.StringWriter writer = new java.io.StringWriter();
        javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(writer);
        transformer.transform(source, result);
        String xmlString = writer.toString();
        
        byte[] byteData = xmlString.getBytes("UTF-8");

        Blob blobData = conn.createBlob();
        blobData.setBytes(1, byteData);
        return blobData;
    }

    /**
     * Insert UBL Header into F564231
     * Corresponds to Invoice header according to EN 16931
     */
    public boolean insertUBLHeader(Document ublDoc, String originalDoc, 
                                   String originalDct, String originalKco, String customerAN8, String customerALKY) 
                                   throws Exception {
        
        String sql = "INSERT INTO " + schema + ".F564231 (" +
                "UHDOC, UHDCT, UHKCO, UHODOC, UHODCT, UHOKCO, UHK74FLEN, UHK74XMLV, UHK74LDDJ, UHDDJ, UHK74LEDT, " +
                "UHATXA, UHSTAM, UHAG, UHAAP, UHCRCD, UHK74MSG1, UH55RSF, UHY74CTID, UHAN8, UHALKY, UHTXFT, " +
                "UHK74INVST, UHRMK, UHSBA1, UHY56PYIN, UHUSER, UHPID, UHJOBN, UHUPMJ, UHTDAY, UHY56RULE" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Primary keys
            stmt.setInt(1, Integer.parseInt(doc));           // DOC
            stmt.setString(2, dct);                            // DCT
            stmt.setString(3, kco);                            // KCO
            
            // Original document reference
            setIntegerOrZero(stmt, 4, originalDoc);                                    // ODOC
            setStringOrBlank(stmt, 5, originalDct);                                        // ODCT
            setStringOrBlank(stmt, 6, originalKco);                                        // OKCO
            
            // UBL Header fields (BT-1, BT-23, BT-2, BT-9, BT-3)
            String invoiceNumber = getXPathValue(ublDoc, "//cbc:ID");
            if (invoiceNumber != null && invoiceNumber.length() > 25) {
                invoiceNumber = invoiceNumber.substring(0, 25);
            }
            setStringOrBlank(stmt, 7, invoiceNumber);                                      // K74FLEN (BT-1)
            setStringOrBlank(stmt, 8, getXPathValue(ublDoc, "//cbc:ProfileID"));           // K74XMLV (BT-23)
            
            String issueDate = getXPathValue(ublDoc, "//cbc:IssueDate");
            stmt.setInt(9, convertToJDEDate(issueDate));                                    // K74LDDJ (BT-2)
            
            String dueDate = getXPathValue(ublDoc, "//cbc:DueDate");
            try {
                stmt.setInt(10, dueDate != null ? convertToJDEDate(dueDate) : 0);          // DDJ (BT-9)
            } catch (Exception e) {
                stmt.setInt(10, 0);
            }
            
            String invoiceTypeCode = getXPathValue(ublDoc, "//cbc:InvoiceTypeCode");
            setStringOrBlank(stmt, 11, invoiceTypeCode);                                    // K74LEDT (BT-3)
            
            // Monetary totals (BT-109, BT-110, BT-112, BT-115)
            Double taxExclusiveAmount = getXPathNumeric(ublDoc, "//cac:LegalMonetaryTotal/cbc:TaxExclusiveAmount");
            setDoubleOrZero(stmt, 12, taxExclusiveAmount);                                 // ATXA (BT-109)
            
            Double taxAmount = getXPathNumeric(ublDoc, "//cac:TaxTotal/cbc:TaxAmount");
            setDoubleOrZero(stmt, 13, taxAmount);                                          // STAM (BT-110)
            
            Double taxInclusiveAmount = getXPathNumeric(ublDoc, "//cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount");
            setDoubleOrZero(stmt, 14, taxInclusiveAmount);                                 // AG (BT-112)
            
            Double payableAmount = getXPathNumeric(ublDoc, "//cac:LegalMonetaryTotal/cbc:PayableAmount");
            setDoubleOrZero(stmt, 15, payableAmount);                                      // AAP (BT-115)
            
            // Currency (BT-5)
            String currency = getXPathValue(ublDoc, "//cbc:DocumentCurrencyCode");
            stmt.setString(16, currency != null ? currency : "EUR");                       // CRCD (BT-5)
            
            // Invoice note (BT-22)
            String note = getXPathValue(ublDoc, "//cbc:Note");
            if (note != null && note.length() > 1024) {
                note = note.substring(0, 1024);
            }
            setStringOrBlank(stmt, 17, note);                                              // K74MSG1 (BT-22)
            
            // Order reference (BT-13)
            String orderRef = getXPathValue(ublDoc, "//cac:OrderReference/cbc:ID");
            setStringOrBlank(stmt, 18, orderRef);                                          // 55RSF (BT-13)
            
            // Contract reference (BT-12)
            String contractRef = getXPathValue(ublDoc, "//cac:ContractDocumentReference/cbc:ID");
            setIntegerOrZero(stmt, 19, contractRef);                                       // Y74CTID (BT-12)
            
            // Customer references
            setIntegerOrZero(stmt, 20, customerAN8);                                       // AN8
            setStringOrBlank(stmt, 21, customerALKY);                                      // ALKY
            
            // Store complete UBL XML
            stmt.setBlob(22, convertNodeToBlob(ublDoc));                               // TXFT
            
            // Status
            String mappedStatus = mapStatusCode("CREATED");
            setStringOrBlank(stmt, 23, mappedStatus);                                      // K74INVST
            
            // Customer Endpoint (BT-49, BT-49-1)
            String endpointID = getXPathValue(ublDoc, "//cac:AccountingCustomerParty/cac:Party/cbc:EndpointID");
            setStringOrBlank(stmt, 24, endpointID);                                        // RMK (BT-49)
            
            String endpointScheme = getXPathValue(ublDoc, "//cac:AccountingCustomerParty/cac:Party/cbc:EndpointID/@schemeID");
            stmt.setString(25, endpointScheme);                                            // SBA1 (BT-49-1)
            
            // Payment means (BT-81)
            String paymentMeansCode = getXPathValue(ublDoc, "//cac:PaymentMeans/cbc:PaymentMeansCode");
            stmt.setString(26, paymentMeansCode);                                         // Y56PYIN (BT-81)
            
            // Audit fields
            stmt.setString(27, "NOMAUBL");                                                 // USER
            stmt.setString(28, "NOMAUBL");                                                 // PID
            stmt.setString(29, "BIP");                                                     // JOBN
            stmt.setInt(30, getCurrentJDEDate());                                          // UPMJ
            stmt.setInt(31, getCurrentJDETime());                                          // TDAY
            setStringOrBlank(stmt, 32, null);                                           // Y56RULE
            
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            // Log database error (ORA-* errors)
            ValidationResult validResult = new ValidationResult();
            String dbErrorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            validResult.addError(new ValidationError("DB", "ERROR", dbErrorMsg, "F564231"));
            insertValidationResults(validResult);

            if (displayError)
                System.out.println(" ** ERROR ** DB ** F564231 : " + e.getMessage());

            return false;
        }
    }

    /**
     * Insert UBL Invoice Lines into F564233
     */
    public boolean insertUBLLines(Document ublDoc) throws Exception {
        
        String sql = "INSERT INTO " + schema + ".F564233 (" +
                "ULDOC, ULDCT, ULKCO, ULLNID, ULDSC1, ULLITM, ULY56QNTY, ULY56UM, ULUPRC, ULATXA, ULREBL, ULCRCD, " +
                "ULK74TVCC, ULTXR1, ULK74EXRC, ULUSER, ULPID, ULJOBN, ULUPMJ, ULTDAY" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try {
            // Get all invoice lines
            XPathExpression linesExpr = xpath.compile("//cac:InvoiceLine");
            NodeList lines = (NodeList) linesExpr.evaluate(ublDoc, XPathConstants.NODESET);
            
            for (int i = 0; i < lines.getLength(); i++) {
                Node lineNode = lines.item(i);
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    // Primary keys
                    stmt.setInt(1, Integer.parseInt(doc));                               // DOC
                    stmt.setString(2, dct);                                                // DCT
                    stmt.setString(3, kco);                                                // KCO
                    
                    // Line ID (BT-126)
                    String lineId = getXPathValue((Node) lineNode, "cbc:ID");
                    stmt.setInt(4, Integer.parseInt(lineId)*1000);                              // LNID (BT-126)
                    
                    // Item description (BT-153)
                    String description = getXPathValue((Node) lineNode, "cac:Item/cbc:Name");
                    if (description != null && description.length() > 40) {
                        description = description.substring(0, 40);
                    }
                    setStringOrBlank(stmt, 5, description);                                // DSC1 (BT-153)
                    
                    // Seller's item ID (BT-155)
                    String itemId = getXPathValue((Node) lineNode, "cac:Item/cac:SellersItemIdentification/cbc:ID");
                    if (itemId != null && itemId.length() > 35) {
                        itemId = itemId.substring(0, 35);
                    }
                    setStringOrBlank(stmt, 6, itemId);                                     // LITM (BT-155)
                    
                    // Quantity (BT-129)
                    String quantityStr = getXPathValue((Node) lineNode, "cbc:InvoicedQuantity");
                    double quantity = scaleNumeric(quantityStr, 10000);
                    stmt.setDouble(7, quantity);                                           // Y56QNTY (BT-129)
                    
                    // Unit of measure (BT-130)
                    String unitCode = getXPathValue((Node) lineNode, "cbc:InvoicedQuantity/@unitCode");
                    setStringOrBlank(stmt, 8, unitCode);                                   // UM (BT-130)
                    
                    // Unit price (BT-146)
                    String priceStr = getXPathValue((Node) lineNode, "cac:Price/cbc:PriceAmount");
                    double price = scaleNumeric(priceStr, 10000);
                    stmt.setDouble(9, price);                                              // UPRC (BT-146)
                    
                    // Line extension amount (BT-131)
                    String lineAmountStr = getXPathValue((Node) lineNode, "cbc:LineExtensionAmount");
                    double lineAmount = scaleNumeric(lineAmountStr, 100);
                    stmt.setDouble(10, lineAmount);                                        // ATXA (BT-131)
                    
                    // Allowance (BT-136, BT-137)
                    String allowanceStr = getXPathValue((Node) lineNode, "cac:AllowanceCharge/cbc:Amount");
                    double allowance = scaleNumeric(allowanceStr, 10000);
                    stmt.setDouble(11, allowance);                                         // REBL (BT-136/137)
                    
                    // Currency
                    String currency = getXPathValue((Node) lineNode, "cbc:LineExtensionAmount/@currencyID");
                    stmt.setString(12, currency != null ? currency : "EUR");               // CRCD
                    
                    // Tax category (BT-151)
                    String taxCategory = getXPathValue((Node) lineNode, "cac:Item/cac:ClassifiedTaxCategory/cbc:ID");
                    setStringOrBlank(stmt, 13, taxCategory);                               // K74TVCC (BT-151)
                    
                    // Tax rate (BT-152)
                    String taxRateStr = getXPathValue((Node) lineNode, "cac:Item/cac:ClassifiedTaxCategory/cbc:Percent");
                    stmt.setDouble(14, scaleNumeric(taxRateStr, 1000));                    // TXR1 (BT-152)
                    
                    // Tax exemption reason (BT-121)
                    String exemptionReason = getXPathValue((Node) lineNode, "cac:Item/cac:ClassifiedTaxCategory/cbc:TaxExemptionReason");
                    setStringOrBlank(stmt, 15, exemptionReason);                           // K74EXRC (BT-121)
                    
                    // Audit fields
                    stmt.setString(16, "NOMAUBL");                                         // USER
                    stmt.setString(17, "NOMAUBL");                                         // PID
                    stmt.setString(18, "BIP");                                             // JOBN
                    stmt.setInt(19, getCurrentJDEDate());                                  // UPMJ
                    stmt.setInt(20, getCurrentJDETime());                                  // TDAY
                    
                    stmt.executeUpdate();
                }
            }
            
            return true;
            
        } catch (Exception e) {
            // Log database error (ORA-* errors)
            ValidationResult validResult = new ValidationResult();
            String dbErrorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            validResult.addError(new ValidationError("DB", "ERROR", dbErrorMsg, "F564233"));
            insertValidationResults(validResult);

            if (displayError)
                System.out.println(" ** ERROR ** DB ** F564233 : " + e.getMessage());

            return false;
        }
    }

    /**
     * Insert VAT Summary into F564234
     */
    public boolean insertVATSummary(Document ublDoc) throws Exception {
        
        String sql = "INSERT INTO " + schema + ".F564234 (" +
                "UVDOC, UVDCT, UVKCO, UVSEQN, UVK74TVCC, UVTXR1, UVATXA, UVSTAM, UVCRCD, UVK74EXRC, " +
                "UVUSER, UVPID, UVJOBN, UVUPMJ, UVTDAY" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try {
            // Get all tax subtotals
            XPathExpression taxSubtotalExpr = xpath.compile("//cac:TaxTotal/cac:TaxSubtotal");
            NodeList taxSubtotals = (NodeList) taxSubtotalExpr.evaluate(ublDoc, XPathConstants.NODESET);
            
            int seqNum = 1;
            for (int i = 0; i < taxSubtotals.getLength(); i++) {
                Node subtotalNode = taxSubtotals.item(i);
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    // Primary keys
                    stmt.setInt(1, Integer.parseInt(doc));                               // DOC
                    stmt.setString(2, dct);                                                // DCT
                    stmt.setString(3, kco);                                                // KCO
                    stmt.setInt(4, seqNum++);                                              // SEQN
                    
                    // Tax category (BT-118)
                    String taxCategory = getXPathValue((Node) subtotalNode, "cac:TaxCategory/cbc:ID");
                    setStringOrBlank(stmt, 5, taxCategory);                                // K74TVCC (BT-118)
                    
                    // Tax rate (BT-119)
                    String taxRateStr = getXPathValue((Node) subtotalNode, "cac:TaxCategory/cbc:Percent");
                    double taxRate = scaleNumeric(taxRateStr, 1000);    // TXR1 (BT-119)
                    stmt.setDouble(6, taxRate); 
                    
                    // Taxable amount (BT-116)
                    String taxableAmountStr = getXPathValue((Node) subtotalNode, "cbc:TaxableAmount");
                    double taxableAmount = scaleNumeric(taxableAmountStr, 100);
                    stmt.setDouble(7, taxableAmount);                                      // ATXA (BT-116)
                    
                    // Tax amount (BT-117)
                    String taxAmountStr = getXPathValue((Node) subtotalNode, "cbc:TaxAmount");
                    double taxAmount = scaleNumeric(taxAmountStr, 100);
                    stmt.setDouble(8, taxAmount);                                          // STAM (BT-117)
                    
                    // Currency
                    String currency = getXPathValue((Node) subtotalNode, "cbc:TaxAmount/@currencyID");
                    stmt.setString(9, currency != null ? currency : "EUR");                // CRCD
                    
                    // Tax exemption reason (BT-120)
                    String exemptionReason = getXPathValue((Node) subtotalNode, "cac:TaxCategory/cbc:TaxExemptionReason");
                    if (exemptionReason != null && exemptionReason.length() > 500) {
                        exemptionReason = exemptionReason.substring(0, 500);
                    }
            
                    setStringOrBlank(stmt, 10, exemptionReason);                           // K74EXRC (BT-120)
                    
                    // Audit fields
                    stmt.setString(11, "NOMAUBL");                                         // USER
                    stmt.setString(12, "NOMAUBL");                                         // PID
                    stmt.setString(13, "BIP");                                             // JOBN
                    stmt.setInt(14, getCurrentJDEDate());                                  // UPMJ
                    stmt.setInt(15, getCurrentJDETime());                                  // TDAY
                    
                    stmt.executeUpdate();
                }
            }
            
            return true;
            
        } catch (Exception e) {
            // Log database error (ORA-* errors)
            ValidationResult validResult = new ValidationResult();
            String dbErrorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            validResult.addError(new ValidationError("DB", "ERROR", dbErrorMsg, "F564234"));
            insertValidationResults(validResult);
            
            if (displayError)
                System.out.println(" ** ERROR ** DB ** F564234 : " + e.getMessage());
            
            return false;
        }
    }

    /**
     * Insert Lifecycle event into F564235
     */
    public boolean insertLifecycleEvent(String statusCode, String message) {
        
        String sql = "INSERT INTO " + schema + ".F564235 (" +
                "USDOC, USDCT, USKCO, USSEQN, USK74RSCD, USK74MSG1, USTRDJ, USUSER, USPID, USJOBN, USUPMJ, USTDAY" +
                ") VALUES (?,?,?,(SELECT NVL(MAX(USSEQN),0)+1 FROM " + schema + ".F564235 WHERE USDOC=? AND USDCT=? AND USKCO=?),?,?,?,?,?,?,?,?)";
        
        String mappedStatus = mapStatusCode(statusCode);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(doc));                                       // DOC
            stmt.setString(2, dct);                                                        // DCT
            stmt.setString(3, kco);                                                        // KCO
            stmt.setInt(4, Integer.parseInt(doc));                                       // For subquery
            stmt.setString(5, dct);                                                        // For subquery
            stmt.setString(6, kco);                                                        // For subquery
            setStringOrBlank(stmt, 7, mappedStatus);                                       // K74RSCD
            
            if (message != null && message.length() > 500) {
                message = message.substring(0, 500);
            }
            setStringOrBlank(stmt, 8, message);                                            // K74MSG1
            stmt.setInt(9, getCurrentJDEDate());                                                  // USER
           
            // Audit fields
            stmt.setString(10, "NOMAUBL");                                                  // USER
            stmt.setString(11, "NOMAUBL");                                                 // PID
            stmt.setString(12, "BIP");                                                     // JOBN
            stmt.setInt(13, getCurrentJDEDate());                                          // UPMJ
            stmt.setInt(14, getCurrentJDETime());                                          // TDAY
            
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            // Log database error (ORA-* errors)
            ValidationResult validResult = new ValidationResult();
            String dbErrorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            validResult.addError(new ValidationError("DB", "ERROR", dbErrorMsg, "F564235"));
            insertValidationResults(validResult);

            if (displayError)
                System.out.println(" ** ERROR ** DB ** F564235 : " + e.getMessage());

            return false;
        }
    }

    /**
     * Insert Validation results into F564236
     */
    public boolean insertValidationResults(ValidationResult validationResult) {
        
        String sql = "INSERT INTO " + schema + ".F564236 (" +
                "  UVDOC, UVDCT, UVKCO, UVSEQN, UVY56LEVEL, UVSRCL, UVY56RULE, UVK74MSG1, UVUSER, UVPID, UVJOBN, UVUPMJ, UVTDAY" +
                ") VALUES (?,?,?,(SELECT NVL(MAX(UVSEQN),0)+1 FROM " + schema + ".F564236 WHERE UVDOC=? AND UVDCT=? AND UVKCO=?),?,?,?,?,?,?,?,?,?)";

        try {          
            for (ValidationError error : validationResult.getErrors()) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, Integer.parseInt(doc));                               // DOC
                    stmt.setString(2, dct);                                                // DCT
                    stmt.setString(3, kco);                                                // KCO
                    stmt.setInt(4, Integer.parseInt(doc));                                       // For subquery
                    stmt.setString(5, dct);                                                        // For subquery
                    stmt.setString(6, kco);                                                        // For subquery
                    setStringOrBlank(stmt, 7, error.getSeverity() != null ? error.getSeverity().toUpperCase() : "");  // Y56LEVEL
                    setStringOrBlank(stmt, 8, error.getSource());                          // SRCL
                    setStringOrBlank(stmt, 9, error.getRuleId());                          // Y56RULE
                    
                    String message = error.getMessage();
                    if (message != null && message.length() > 2000) {
                        message = message.substring(0, 2000);
                    }
                    setStringOrBlank(stmt, 10, message);                                    // K74MSG1
                    
                    // Audit fields
                    stmt.setString(11, "NOMAUBL");                                          // USER
                    stmt.setString(12, "NOMAUBL");                                         // PID
                    stmt.setString(13, "BIP");                                             // JOBN
                    stmt.setInt(14, getCurrentJDEDate());                                  // UPMJ
                    stmt.setInt(15, getCurrentJDETime());                                  // TDAY
                    
                    stmt.executeUpdate();
                }
            }
            
            return true;
            
        } catch (SQLException e) {
            // Log database error (ORA-* errors)
            ValidationResult validResult = new ValidationResult();
            String dbErrorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            validResult.addError(new ValidationError("DB", "ERROR", dbErrorMsg, "F564236"));
            insertValidationResults(validResult);

            if (displayError)
                System.out.println(" ** ERROR ** DB ** F564236 : " + e.getMessage());

            return false;
        }
    }

    /**
     * Update invoice status in F564231
     */
    public boolean updateInvoiceStatus(String status) {
        
        String sql = "UPDATE " + schema + ".F564231 SET UHK74INVST=?, UHUPMJ=?, UHTDAY=? WHERE UHDOC=? AND UHDCT=? AND UHKCO=?";

        String mappedStatus = mapStatusCode(status);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setStringOrBlank(stmt, 1, mappedStatus);                                       // UHK74INVST
            stmt.setInt(2, getCurrentJDEDate());                                           // UHUPMJ
            stmt.setInt(3, getCurrentJDETime());                                           // UHTDAY
            stmt.setInt(4, Integer.parseInt(doc));                                       // USDOC
            stmt.setString(5, dct);                                                        // USDCT
            stmt.setString(6, kco);                                                        // USKCO
            
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                return true;
            } else {
                return false;
            }
            
        } catch (SQLException e) {           
            // Log database error (ORA-* errors)
            ValidationResult validResult = new ValidationResult();
            String dbErrorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            validResult.addError(new ValidationError("DB", "ERROR", dbErrorMsg, "F564231"));
            insertValidationResults(validResult);

            if (displayError)
                System.out.println(" ** ERROR ** DB ** F564231 : " + e.getMessage());

            return false;
        }
    }

    /**
     * Helper method to extract XPath value from Node
     */
    private String getXPathValue(Node node, String expression) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(expression);
        String value = (String) expr.evaluate(node, XPathConstants.STRING);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    /**
     * Insert document into F564230 log table
     * 
     * @param docID Document ID (mandatory)
     * @param activite Activity code (mandatory - FEAA10)
     * @param typePiece Document type (mandatory - FEAA20)
     * @param typeJDE JDE type (mandatory - FEDCT)
     * @param societeJDE JDE company code (mandatory - FEKCO)
     * @param element XML element containing document data
     * @param numClientTag Tag name for client number
     * @param montantTag Tag name for amount
     * @param datePieceTag Tag name for document date
     * @param dateEcheanceTag Tag name for due date
     * @param codeRoutageTag Tag name for routing code
     * @param fileName Source file name
     * @param tableLog Log table name
     * @return true if successful
     */
    public boolean insertDocumentLog(String activite, String typePiece, Element element, String numClientTag, String montantTag, String datePieceTag,
            String dateEcheanceTag, String codeRoutageTag, String fileName, String tableLog) {
        
        try {
            // Validate mandatory fields
            if (doc == null || doc.trim().isEmpty()) {
                throw new IllegalArgumentException("FEDOC (docID) is mandatory");
            }
            if (dct == null || dct.trim().isEmpty()) {
                throw new IllegalArgumentException("FEDCT (typeJDE) is mandatory");
            }
            if (kco == null || kco.trim().isEmpty()) {
                throw new IllegalArgumentException("FEKCO (societeJDE) is mandatory");
            }
            if (activite == null || activite.trim().isEmpty()) {
                throw new IllegalArgumentException("FEAA10 (activite) is mandatory");
            }
            if (typePiece == null || typePiece.trim().isEmpty()) {
                throw new IllegalArgumentException("FEAA20 (typePiece) is mandatory");
            }
            
            String numClient = getNodeString(numClientTag, element);
            String montant = getNodeString(montantTag, element);
            String datePiece = getNodeString(datePieceTag, element);
            String dateEcheance = getNodeString(dateEcheanceTag, element);
            String codeRoutage = getNodeString(codeRoutageTag, element);

            String sql = "INSERT INTO " + schema + "." + tableLog
                    + " (FEDOC, FEDCT, FEKCO, FEAA10, FEAA20, FEALKY, FEAEXP, FEIVD, FEARDU, FEUPMJ, FEPID, FEVERS, FEUSER, FEJOBN, FEUPMT, FEWDS1, FEEV01, FETXFT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Mandatory fields
                stmt.setInt(1, Integer.parseInt(doc));                                   // FEDOC
                stmt.setString(2, dct);                                                // FEDCT
                stmt.setString(3, kco);                                             // FEKCO
                stmt.setString(4, activite);                                               // FEAA10
                stmt.setString(5, typePiece);                                              // FEAA20
                
                // Optional fields with null handling
                setStringOrBlank(stmt, 6, numClient);                                      // FEALKY
                
                // Amount with null handling
                if (montant != null && !montant.trim().isEmpty()) {
                    double amount = scaleNumeric(montant.replace(",", "."), 100);
                    stmt.setDouble(7, amount);                                             // FEAEXP
                } else {
                    stmt.setDouble(7, 0.0);
                }
                
                // Dates with null handling
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyDDD");
                SimpleDateFormat sdf3 = new SimpleDateFormat("HHmmss");
                
                if (datePiece != null && !datePiece.trim().isEmpty()) {
                    Date dateTmp = sdf1.parse(datePiece);
                    stmt.setInt(8, Integer.parseInt(sdf2.format(dateTmp)) - 1900000);     // FEIVD
                } else {
                    stmt.setInt(8, 0);
                }
                
                if (dateEcheance != null && !dateEcheance.trim().isEmpty()) {
                    Date dateTmp = sdf1.parse(dateEcheance);
                    stmt.setInt(9, Integer.parseInt(sdf2.format(dateTmp)) - 1900000);     // FEARDU
                } else {
                    stmt.setInt(9, 0);
                }
                
                // Current date and time
                Date date = new Date();
                stmt.setInt(10, Integer.parseInt(sdf2.format(date)) - 1900000);           // FEUPMJ
                
                // Audit fields
                stmt.setString(11, "JAVA");                                                // FEPID
                stmt.setString(12, "V1.0");                                                // FEVERS
                stmt.setString(13, "JDEBIP");                                              // FEUSER
                stmt.setString(14, "BIP");                                                 // FEJOBN
                stmt.setInt(15, Integer.parseInt(sdf3.format(date)));                     // FEUPMT
                setStringOrBlank(stmt, 16, fileName);                                      // FEWDS1
                setStringOrBlank(stmt, 17, codeRoutage);                                   // FEEV01
                
                // XML blob
                stmt.setBlob(18, convertNodeToBlob(element));                          // FETXFT

                stmt.executeUpdate();
                return true;
            }
            
        } catch (IllegalArgumentException e) {
            System.err.println(" ** ERROR ** DB ** " + tableLog + " : " + e.getMessage());
            
            return false;
        } catch (Exception e) {
            // Log database error (ORA-* errors)
            ValidationResult validResult = new ValidationResult();
            String dbErrorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            validResult.addError(new ValidationError("DB", "ERROR", dbErrorMsg, "F564230"));
            insertValidationResults(validResult);
            
            if (displayError)
                System.out.println(" ** ERROR ** DB ** F564230 : " + e.getMessage());
            
            return false;
        }
    }


    /**
     * Get node string value (helper for legacy methods)
     */
    private String getNodeString(String tagName, Element element) {
        NodeList listNode = element.getElementsByTagName(tagName);
        if (listNode != null && listNode.getLength() > 0) {
            NodeList subList = listNode.item(0).getChildNodes();
            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }
        return null;
    }

    /**
     * Convert Node to Blob (helper for legacy methods)
     */
    private Blob convertNodeToBlob(Node n) throws Exception {
        Blob blobData = conn.createBlob();
        try {
            javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
            javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(n);
            javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(new java.io.StringWriter());
            transformer.transform(source, result);
            String strObject = result.getWriter().toString();
            byte[] byteData = strObject.getBytes("UTF-8");
            blobData.setBytes(1, byteData);
        } catch (Exception e) {
            throw new Exception("Failed to convert node to blob: " + e.getMessage());
        }
        return blobData;
    }
}
