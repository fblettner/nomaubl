/*
 * Copyright (c) 2018 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package custom.ubl;

import java.io.*;
import java.util.Base64;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.util.concurrent.Callable;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import custom.resources.*;
import static custom.resources.Tools.decodePasswd;
import java.sql.*;
import oracle.xdo.XDOException;
import oracle.xdo.template.FOProcessor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CustomUBL implements Callable<Integer> {

    private final boolean displayError;
    private final int startInvoice;
    private final int endInvoice;
    private final NodeList list;
    private final ByteArrayOutputStream xslOutStream;
    private String pAppHome;
    private String pSetLocale;
    private String pProcessHome;
    private final String pTemplate;
    private final String pParamType;
    private final String pFileName;
    private final String configFile;
    private String pdoc;
    private String pActivite;
    private String pTypePiece;
    private String pdct;
    private String pkco;
    private String pNumClient;
    private String pMontant;
    private String pDatePiece;
    private String pDateEcheance;
    private String pXslTemplate;
    private String pTempOutput;
    private String pDirOutput;
    private String pDirInput;
    private String pRunGS;
    private String pCmdGS;
    private final String TEMPLATE = "%TEMPLATE%";
    private final String FILE_NAME = "%FILE_NAME%";
    private final String APP_HOME = "%APP_HOME%";
    private final String PROCESS_HOME = "%PROCESS_HOME%";
    private String pURL;
    private String pSchema;
    private String pUpdateDB;
    private String pTableLog;
    private String pDBUser;
    private String pDBPasswd;
    private String pCodeRoutage;
    private static String pXdoConfig;
    private String pUblXsltPath;
    private UBLValidator pUBLValidator;
    private TokenManager pTokenManager;
    private String pAttachment;
    private String pSendToPA;
    private String pPaMode;
    private String pPaApiBaseUrl;
    private String pPaApiImportEndpoint;
    private int pPaApiTimeout;
    private String pUblConfigPath;

    /**
     * Generic logging function following standard format: ** LEVEL ** MODULE **
     * SUBMODULE : message
     * 
     * @param level     Log level (INFO, SUCCESS, WARNING, ERROR)
     * @param module    Module name (UBL, DB, PA, etc.)
     * @param submodule Submodule or component name
     * @param message   Log message
     */
    private void log(String level, String module, String submodule, String message) {
        String logMsg = String.format(" ** %s ** %s ** %s : %s",
                level.toUpperCase(),
                module.toUpperCase(),
                submodule,
                message);

        if (displayError) {
            if ("ERROR".equalsIgnoreCase(level)) {
                System.err.println(logMsg);
            } else {
                System.out.println(logMsg);
            }
        }
    }

    private String replaceConstValue(String inputStr) {
        String replaceStr = inputStr.replace(APP_HOME, pAppHome);
        replaceStr = replaceStr.replace(PROCESS_HOME, pProcessHome);
        replaceStr = replaceStr.replace(TEMPLATE, pTemplate);
        replaceStr = replaceStr.replace(FILE_NAME, pFileName);
        return replaceStr;
    }

    // Déclaration des variables
    public CustomUBL(int startI, int endI, NodeList inputNode, ByteArrayOutputStream baos, String inTmpl,
            String inFileName, String inConfig, String inParamType, UBLValidator inUBLValidator,
            TokenManager inTokenManager, boolean inDisplayError) {
        displayError = inDisplayError || (pUpdateDB != null && pUpdateDB.equalsIgnoreCase("N"));
        startInvoice = startI;
        endInvoice = endI;
        list = inputNode;
        xslOutStream = baos;
        pTemplate = inTmpl;
        pFileName = inFileName;
        configFile = inConfig;
        pParamType = inParamType;
        pUBLValidator = inUBLValidator;
        pTokenManager = inTokenManager;
    }

    // Chargement du fichier de configuration
    private void Init() {
        try {
            File file = new File(configFile);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);

            Resource resource = resources.getResourceByName("global");
            pAppHome = resource.getProperty("appHome");
            pProcessHome = resource.getProperty("processHome");
            pTempOutput = replaceConstValue(resource.getProperty("tempOutput"));
            pDirOutput = replaceConstValue(resource.getProperty("dirOutput"));
            pDirInput = replaceConstValue(resource.getProperty("dirInput"));
            pRunGS = resource.getProperty("runGS");
            pCmdGS = resource.getProperty("cmdGS");
            pURL = resource.getProperty("URL");
            pSchema = resource.getProperty("schema");
            pUpdateDB = resource.getProperty("updateDB");
            pTableLog = resource.getProperty("tableLog");
            pSetLocale = resource.getProperty("setLocale");
            pDBUser = resource.getProperty("DBUser");
            pDBPasswd = decodePasswd(resource.getProperty("DBPassword"));
            pXdoConfig = resource.getProperty("xdo");
            pUblConfigPath = replaceConstValue(resource.getProperty("ublConfigPath"));

            resource = resources.getResourceByName(pTemplate);
            pdoc = resource.getProperty("docID");
            pActivite = resource.getProperty("activite");
            pTypePiece = resource.getProperty("typePiece");
            pdct = resource.getProperty("typeJDE");
            pkco = resource.getProperty("societeJDE");
            pNumClient = resource.getProperty("numClient");
            pMontant = resource.getProperty("montant");
            pDatePiece = resource.getProperty("datePiece");
            pDateEcheance = resource.getProperty("dateEcheance");
            pXslTemplate = replaceConstValue(resource.getProperty("xsl"));
            pCodeRoutage = resource.getProperty("codeRoutage");
            pUblXsltPath = replaceConstValue(resource.getProperty("ublXslt"));
            pAttachment = resource.getProperty("attachment");

            // Load PA API configuration from global template
            resource = resources.getResourceByName("global");
            pSendToPA = resource.getProperty("sendToPA");
            pPaMode = resource.getProperty("paMode");
            pPaApiBaseUrl = resource.getProperty("paApiBaseUrl");
            pPaApiImportEndpoint = resource.getProperty("paApiImportEndpoint");
            String timeout = resource.getProperty("paApiTimeout");
            pPaApiTimeout = (timeout != null) ? Integer.parseInt(timeout) : 30000;

        } catch (Exception e) {
            e.printStackTrace();
            // System.exit(1);
        }
    }

    // Récupérer la valeur d'un tag XML
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

    // Mise en forme PDF via les API BI Publisher
    private Boolean convertToPDF(InputStream xmlStream, String outputPDF) {
        try {
            FOProcessor processor = new FOProcessor();

            // input XML
            processor.setData(xmlStream);
            try ( // input XSL template
                    ByteArrayInputStream xslInStream = new ByteArrayInputStream(xslOutStream.toByteArray())) {
                processor.setTemplate(xslInStream);
                // output PDF
                processor.setOutput(outputPDF);
                // Format de sortie
                processor.setOutputFormat(FOProcessor.FORMAT_PDF);
                processor.setConfig(pXdoConfig);
                processor.setLocale(pSetLocale);
                // Traitement
                processor.generate();
            }

        } catch (IOException | XDOException e) {
            return false;
        }
        return true;
    }

    private Boolean convertToXML(InputStream xmlStream, String outputXML) {
        try {
            // Use the factory to create a template containing the xsl file
            TransformerFactory factory = TransformerFactory.newInstance();

            Source xml = new StreamSource(xmlStream);
            Source xsl = new StreamSource("file:" + pXslTemplate);
            Templates template = factory.newTemplates(xsl);
            Transformer xformer = template.newTransformer();

            FileOutputStream fos = new FileOutputStream(outputXML);
            Result result = new StreamResult(fos);

            xformer.transform(xml, result);
            fos.close();
        } catch (IOException | TransformerException e) {
            return false;
        }
        return true;
    }

    private void executeGS(String inputGS) {

        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(inputGS);
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private boolean convertToUBL(InputStream invoiceXmlStream, String outputUblFile) {
        try {
            // Use Saxon for XSLT 2.0 support
            TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
            Source xml = new StreamSource(invoiceXmlStream);
            Source xsl = new StreamSource("file:" + pUblXsltPath);
            Templates template = factory.newTemplates(xsl);
            Transformer transformer = template.newTransformer();

            try (FileOutputStream fos = new FileOutputStream(outputUblFile)) {
                Result result = new StreamResult(fos);
                transformer.transform(xml, result);
            }
            return true;
        } catch (IOException | TransformerException e) {
            return false;
        }
    }

    private Document parseUBLFile(String ublFilePath) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new File(ublFilePath));
    }

    /**
     * Sends UBL file to the Platform Agréée (PA) via API
     * 
     * @param ublFilePath Path to the UBL XML file
     * @param docName     Document name for logging
     * @return true if successful, false otherwise
     */
    private boolean sendToPlatform(String ublFilePath, String docName) {
        if (!"API".equalsIgnoreCase(pPaMode)) {
            log("INFO", "PA", "Mode", "not API, skipping send for " + docName);
            return true;
        }

        if (pTokenManager == null) {
            log("ERROR", "PA", "TokenManager", "not initialized for " + docName);
            return false;
        }

        try {
            // Read UBL file and encode to base64
            File ublFile = new File(ublFilePath);
            byte[] ublBytes = new byte[(int) ublFile.length()];
            try (FileInputStream fis = new FileInputStream(ublFile)) {
                fis.read(ublBytes);
            }
            String base64Ubl = Base64.getEncoder().encodeToString(ublBytes);

            // Try sending with current token, retry once with refreshed token if 401
            for (int attempt = 0; attempt < 2; attempt++) {
                String token = pTokenManager.getToken();
                if (token == null) {
                    log("ERROR", "PA", "Auth", "Failed to get auth token for " + docName);
                    return false;
                }

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(pPaApiTimeout))
                        .build();

                String jsonPayload = String.format(
                        "{\"format\":\"xml_ubl\",\"content\":\"%s\",\"postActions\":[]}",
                        base64Ubl);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(pPaApiBaseUrl + pPaApiImportEndpoint))
                        .timeout(Duration.ofMillis(pPaApiTimeout))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log("SUCCESS", "UBL", "PA", "Document sent successfully: " + docName);
                    log("INFO", "UBL", "PA", "Response: " + response.body());
                    return true;
                } else if (response.statusCode() == 401 && attempt == 0) {
                    // Token expired, refresh and retry
                    log("WARNING", "UBL", "PA", "Token expired, refreshing and retrying for " + docName);
                    pTokenManager.refreshToken();
                    continue;
                } else {
                    log("ERROR", "UBL", "PA",
                            "Failed to send document " + docName + " - Status: " + response.statusCode());
                    log("ERROR", "UBL", "PA", "Response: " + response.body());
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            log("ERROR", "UBL", "PA", "Exception sending document " + docName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Embeds a PDF file as base64 in the UBL XML document
     * 
     * @param ublFilePath Path to the UBL XML file
     * @param pdfFilePath Path to the PDF file to embed
     * @param pdfFileName Filename to use in the attachment
     * @return true if successful, false otherwise
     */
    private boolean embedPdfInUBL(String ublFilePath, String pdfFilePath, String pdfFileName) {
        try {
            // Read PDF file and encode to base64
            File pdfFile = new File(pdfFilePath);
            if (!pdfFile.exists()) {
                System.err.println("PDF file not found: " + pdfFilePath);
                return false;
            }

            byte[] pdfBytes = new byte[(int) pdfFile.length()];
            try (FileInputStream fis = new FileInputStream(pdfFile)) {
                fis.read(pdfBytes);
            }
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            // Parse UBL XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(ublFilePath));

            Element root = doc.getDocumentElement();
            String cacNamespace = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
            String cbcNamespace = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";

            // Create AdditionalDocumentReference element
            Element additionalDocRef = doc.createElementNS(cacNamespace, "cac:AdditionalDocumentReference");

            // Add ID element
            Element id = doc.createElementNS(cbcNamespace, "cbc:ID");
            id.setTextContent("PDF_Invoice");
            additionalDocRef.appendChild(id);

            // Create Attachment element
            Element attachment = doc.createElementNS(cacNamespace, "cac:Attachment");

            // Create EmbeddedDocumentBinaryObject element
            Element embeddedDoc = doc.createElementNS(cbcNamespace, "cbc:EmbeddedDocumentBinaryObject");
            embeddedDoc.setAttribute("mimeCode", "application/pdf");
            embeddedDoc.setAttribute("filename", pdfFileName);
            embeddedDoc.setTextContent(base64Pdf);

            attachment.appendChild(embeddedDoc);
            additionalDocRef.appendChild(attachment);

            // Insert AdditionalDocumentReference after the last existing one or before
            // UBLExtensions if no references exist
            NodeList existingRefs = root.getElementsByTagNameNS(cacNamespace, "AdditionalDocumentReference");
            if (existingRefs.getLength() > 0) {
                // Insert after the last existing reference
                Node lastRef = existingRefs.item(existingRefs.getLength() - 1);
                Node nextSibling = lastRef.getNextSibling();
                if (nextSibling != null) {
                    root.insertBefore(additionalDocRef, nextSibling);
                } else {
                    root.appendChild(additionalDocRef);
                }
            } else {
                // Insert before AccountingSupplierParty or at a reasonable position
                NodeList supplierParty = root.getElementsByTagNameNS(cacNamespace, "AccountingSupplierParty");
                if (supplierParty.getLength() > 0) {
                    root.insertBefore(additionalDocRef, supplierParty.item(0));
                } else {
                    // Just append to root
                    root.appendChild(additionalDocRef);
                }
            }

            // Write modified UBL back to file
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(ublFilePath));
            transformer.transform(source, result);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Integer call() throws Exception {

        Init();

        TransformerFactory tranFactory = TransformerFactory.newInstance();
        Transformer aTransformer = tranFactory.newTransformer();

        Connection conn = null;
        if (pUpdateDB.equals("Y")) {
            // register oracle driver
            Class.forName("oracle.jdbc.OracleDriver");
            // connect to oracle and login
            conn = DriverManager.getConnection(pURL, pDBUser, pDBPasswd);
        }

        for (int i = startInvoice; i < endInvoice; i++) {
            Node element = list.item(i).cloneNode(true);

            if (element.hasChildNodes()) {
                Source src = new DOMSource(element);
                try {
                    String doc = getNodeString(pdoc, (Element) element);
                    String dct = getNodeString(pdct, (Element) element);
                    String kco = getNodeString(pkco, (Element) element);
                    String activite = getNodeString(pActivite, (Element) element);
                    String typePiece = getNodeString(pTypePiece, (Element) element);

                    // Create database handler for legacy tables
                    UBLDatabaseHandler dbHandler = new UBLDatabaseHandler(conn, pSchema, doc, dct, kco, pUblConfigPath, displayError);

                    boolean isDocOK = true;

                    // INSERT table F564230
                    if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                        try {
                            isDocOK = dbHandler.insertDocumentLog(activite, typePiece, 
                                    (Element) element, pNumClient, pMontant, pDatePiece, pDateEcheance, pCodeRoutage,
                                    pFileName, pTableLog);
                        } catch (Exception e) {
                            log("ERROR", "DB", "INSERT", "Insert failed: " + e.getMessage());
                        }
                    }

                    if (isDocOK) {
                        String docName = activite + "_" + typePiece + "_" + doc + "_" + dct + "_" + kco;

                        InputStream is;
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                            Result outputTarget = new StreamResult(outputStream);
                            aTransformer.transform(src, outputTarget);
                            is = new ByteArrayInputStream(outputStream.toByteArray());

                            if (pParamType.equals("BURST") || pParamType.equals("BOTH")
                                    || (pAttachment != null && pAttachment.equals("create"))) {
                                // Recreate InputStream for PDF generation (consumed by UBL in BOTH mode)
                                is = new ByteArrayInputStream(outputStream.toByteArray());

                                if (!convertToPDF(is, pTempOutput + docName + ".pdf")) {
                                    if (pUpdateDB.equals("Y")) {
                                        ValidationResult errResult = new ValidationResult();
                                        errResult.addError(new ValidationError("PDF", "ERROR", "ERREUR CREATION PDF",
                                                "PDF_CREATION"));
                                        dbHandler.insertValidationResults(errResult);
                                    }
                                } else {

                                    String gsExec = "cp " + pTempOutput + docName + ".pdf " + pDirOutput +
                                            docName + ".pdf";
                                    if (pRunGS.equals("Y")) {
                                        gsExec = pCmdGS + pDirOutput + docName + ".pdf " + pTempOutput + docName +
                                                ".pdf";
                                    }
                                    executeGS(gsExec);

                                    is = new ByteArrayInputStream(outputStream.toByteArray());
                                    if (!convertToXML(is, pDirOutput + docName + ".xml")) {
                                        if (pUpdateDB.equals("Y")) {
                                            ValidationResult errResult = new ValidationResult();
                                            errResult.addError(new ValidationError("XML", "ERROR",
                                                    "ERREUR CREATION INDEX", "XML_CREATION"));
                                            dbHandler.insertValidationResults(errResult);
                                        }
                                    }

                                }

                            }

                            if (pParamType.equals("UBL") || pParamType.equals("BOTH")
                                    || pParamType.equals("UBL_VALIDATE")) {
                                // Recreate InputStream for UBL conversion (may have been consumed by PDF/XML
                                // generation)
                                is = new ByteArrayInputStream(outputStream.toByteArray());

                                String ublFile = pDirOutput + docName + "_ubl.xml";
                                if (!convertToUBL(is, ublFile)) {
                                    if (pUpdateDB.equals("Y")) {
                                        ValidationResult errResult = new ValidationResult();
                                        errResult.addError(new ValidationError("UBL", "ERROR", "ERREUR CREATION UBL",
                                                "UBL_CREATION"));
                                        dbHandler.insertValidationResults(errResult);
                                    }
                                } else {
                                    // Add PDF attachment if required (not in validation-only mode)
                                    if (!pParamType.equals("UBL_VALIDATE") && pAttachment != null
                                            && (pAttachment.equals("create") || pAttachment.equals("attach"))) {
                                        String pdfFile = pDirInput + docName + ".pdf";
                                        if (pAttachment.equals("create"))
                                            pdfFile = pDirOutput + docName + ".pdf";

                                        String pdfFileName = docName + ".pdf";
                                        if (!embedPdfInUBL(ublFile, pdfFile, pdfFileName)) {
                                            log("WARNING", "UBL", "Attachment",
                                                    "Could not embed PDF attachment in UBL for " + docName);
                                        } else {
                                            log("SUCCESS", "UBL", "Attachment",
                                                    "PDF attachment embedded in UBL for " + docName);
                                        }
                                    }

                                    Document ublDoc = parseUBLFile(ublFile);
                                    ValidationResult validResult = pUBLValidator.validateUbl(ublDoc);

                                    // Populate UBL tables if enabled (before sending to PA)
                                    if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                        try {

                                            // Insert lifecycle event: CREATED
                                            dbHandler.insertLifecycleEvent("CREATED",
                                                    "Invoice created in JDE");

                                            // Insert header
                                            String numClient = getNodeString(pNumClient, (Element) element);
                                            if (dbHandler.insertUBLHeader(ublDoc, 
                                                    null, null, null, null, numClient)) {
                                                // Insert lines
                                                dbHandler.insertUBLLines(ublDoc);

                                                // Insert VAT summary
                                                dbHandler.insertVATSummary(ublDoc);

                                                // Insert validation results
                                                dbHandler.insertValidationResults(
                                                        validResult);
                                            }
                                        } catch (Exception e) {
                                            log("ERROR", "DB", "UBL Tables",
                                                    "Failed to populate UBL tables for " + docName);
                                            log("ERROR", "DB", "UBL Tables", e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }

                                    if (!validResult.isValid()) {
                                        // Display all errors/warnings
                                        for (ValidationError e : validResult.getErrors()) {
                                            String ruleId = e.getRuleId() != null ? e.getRuleId() : "UNDEFINED";
                                            log(e.getSeverity(), e.getSource(), ruleId, e.getMessage());
                                        }

                                        // Update status to VALIDATED (with warnings) if UBL tables populated
                                        if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                            try {
                                                dbHandler.updateInvoiceStatus(
                                                        "VALIDATED_WARN");
                                                dbHandler.insertLifecycleEvent(
                                                        "VALIDATED_WARN",
                                                        "Validation completed with warnings");
                                            } catch (Exception e) {
                                                log("ERROR", "DB", "Status", "Update failed: " + e.getMessage());
                                            }
                                        }

                                        // Check if only warnings (no errors)
                                        boolean hasOnlyWarnings = true;
                                        for (ValidationError e : validResult.getErrors()) {
                                            if (!"WARNING".equalsIgnoreCase(e.getSeverity().toUpperCase())) {
                                                hasOnlyWarnings = false;
                                                break;
                                            }
                                        }

                                        // Send to PA if F (force) mode and only warnings (not in validation-only mode)
                                        if (!pParamType.equals("UBL_VALIDATE") && "F".equalsIgnoreCase(pSendToPA)
                                                && hasOnlyWarnings) {
                                            log("INFO", "UBL", "PA",
                                                    "forcing send to PA despite warnings for " + docName);

                                            // Update status before sending
                                            if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                try {
                                                    dbHandler.updateInvoiceStatus("SENT");
                                                    dbHandler.insertLifecycleEvent("SENT",
                                                            "Sent to PA");
                                                } catch (Exception e) {
                                                    log("ERROR", "DB", "Status", "Update failed: " + e.getMessage());
                                                }
                                            }

                                            if (!sendToPlatform(ublFile, docName)) {
                                                if (pUpdateDB.equals("Y")) {
                                                    ValidationResult errResult = new ValidationResult();
                                                    errResult.addError(new ValidationError("PA", "ERROR",
                                                            "ERREUR ENVOI PA", "PA_SEND"));
                                                    dbHandler.insertValidationResults(
                                                            errResult);
                                                }

                                                // Update status to ERROR
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        dbHandler.updateInvoiceStatus(
                                                                "ERROR");
                                                        dbHandler.insertLifecycleEvent(
                                                                "ERROR",
                                                                "Failed to send to PA");
                                                    } catch (Exception e) {
                                                        log("ERROR", "DB", "Status",
                                                                "Update failed: " + e.getMessage());
                                                    }
                                                }
                                            } else {
                                                // Update status to DEPOSEE (deposited)
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        dbHandler.updateInvoiceStatus(
                                                                "DEPOSEE");
                                                        dbHandler.insertLifecycleEvent(
                                                                "DEPOSEE", "Deposited on PA");
                                                    } catch (Exception e) {
                                                        log("ERROR", "DB", "Status",
                                                                "Update failed: " + e.getMessage());
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        log("SUCCESS", "UBL", typePiece, "validation successful for " + docName);

                                        // Update status to VALIDATED if UBL tables populated
                                        if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                            try {
                                                dbHandler.updateInvoiceStatus("VALIDATED");
                                                dbHandler.insertLifecycleEvent("VALIDATED",
                                                        "Validation successful");
                                            } catch (Exception e) {
                                                log("ERROR", "DB", "Status", "Update failed: " + e.getMessage());
                                            }
                                        }

                                        // Send to PA if enabled (Y or F mode) - not in validation-only mode
                                        if (!pParamType.equals("UBL_VALIDATE")
                                                && ("Y".equalsIgnoreCase(pSendToPA)
                                                        || "F".equalsIgnoreCase(pSendToPA))) {

                                            // Update status before sending
                                            if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                try {
                                                    dbHandler.updateInvoiceStatus("SENT");
                                                    dbHandler.insertLifecycleEvent("SENT",
                                                            "Sent to PA");
                                                } catch (Exception e) {
                                                    log("ERROR", "DB", "Status", "Update failed: " + e.getMessage());
                                                }
                                            }

                                            if (!sendToPlatform(ublFile, docName)) {
                                                if (pUpdateDB.equals("Y")) {
                                                    ValidationResult errResult = new ValidationResult();
                                                    errResult.addError(new ValidationError("PA", "ERROR",
                                                            "ERREUR ENVOI PA", "PA_SEND"));
                                                    dbHandler.insertValidationResults(
                                                            errResult);
                                                }

                                                // Update status to ERROR
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        dbHandler.updateInvoiceStatus(
                                                                "ERROR");
                                                        dbHandler.insertLifecycleEvent(
                                                                "ERROR",
                                                                "Failed to send to PA");
                                                    } catch (Exception e) {
                                                        log("ERROR", "DB", "Status",
                                                                "Update failed: " + e.getMessage());
                                                    }
                                                }
                                            } else {
                                                // Update status to DEPOSEE (deposited)
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        dbHandler.updateInvoiceStatus("DEPOSEE");
                                                        dbHandler.insertLifecycleEvent("DEPOSEE",
                                                                "Deposited on PA");
                                                    } catch (Exception e) {
                                                        log("ERROR", "DB", "Status",
                                                                "Update failed: " + e.getMessage());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        is.close();
                    }
                } catch (IOException | TransformerException | Error e) {
                    System.out.println(e);
                    throw new Exception("Thread interrompu ; cause " + i + " / " + e.getMessage());
                }
            }
        }
        if (pUpdateDB.equals("Y"))
            conn.close();
        return 0;
    }

}
