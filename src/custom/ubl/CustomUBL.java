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

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.util.concurrent.Callable;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import custom.resources.*;
import static custom.resources.Tools.decodePasswd;
import java.sql.*;
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

    /**
     * Log using a LogEntry from LogCatalog
     * 
     * @param entry LogEntry to log
     */
    private void log(LogCatalog.LogEntry entry) {
        entry.print(displayError);
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

    /**
     * Sends UBL file to the Platform Agréée (PA) via API
     * 
     * @param ublFilePath Path to the UBL XML file
     * @param docName     Document name for logging
     * @return true if successful, false otherwise
     */
    private boolean sendToPlatform(String ublFilePath, String docName) {
        if (!"API".equalsIgnoreCase(pPaMode)) {
            log(LogCatalog.paNotApi(docName));
            return true;
        }

        if (pTokenManager == null) {
            log(LogCatalog.paTokenManagerNotInitialized(docName));
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
                    log(LogCatalog.paAuthFailed(docName));
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
                    log(LogCatalog.paDocumentSent(docName));
                    log(LogCatalog.info(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, "Response: " + response.body()));
                    return true;
                } else if (response.statusCode() == 401 && attempt == 0) {
                    // Token expired, refresh and retry
                    log(LogCatalog.paTokenExpired(docName));
                    pTokenManager.refreshToken();
                    continue;
                } else {
                    log(LogCatalog.paSendError(docName, response.statusCode()));
                    log(LogCatalog.error(LogCatalog.MODULE_UBL, LogCatalog.SUB_UBL_PA, "Response: " + response.body()));
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            log(LogCatalog.paSendException(docName, e.getMessage()));
            e.printStackTrace();
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
                    String doc = Tools.getNodeString(pdoc, (Element) element);
                    String dct = Tools.getNodeString(pdct, (Element) element);
                    String kco = Tools.getNodeString(pkco, (Element) element);
                    String activite = Tools.getNodeString(pActivite, (Element) element);
                    String typePiece = Tools.getNodeString(pTypePiece, (Element) element);
                    // Create database handler for legacy tables
                    UBLDatabaseHandler dbHandler = new UBLDatabaseHandler(conn, pSchema, doc, dct, kco, displayError);

                    boolean isDocOK = true;

                    // INSERT table F564230
                    if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                        try {
                            isDocOK = dbHandler.insertDocumentLog(activite, typePiece,
                                    (Element) element, pNumClient, pMontant, pDatePiece, pDateEcheance, pCodeRoutage,
                                    pFileName, pTableLog);
                        } catch (Exception e) {
                            log(LogCatalog.dbInsertFailed(e.getMessage()));
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

                                if (!BIPublisher.convertToPDF(is, pTempOutput + docName + ".pdf", xslOutStream,
                                        pXdoConfig, pSetLocale)) {
                                    if (pUpdateDB.equals("Y")) {
                                        ValidationResult errResult = new ValidationResult();
                                        errResult.addError(ErrorCatalog.pdfCreationError());
                                        dbHandler.insertValidationResults(errResult);
                                    }
                                } else {

                                    String gsExec = "cp " + pTempOutput + docName + ".pdf " + pDirOutput +
                                            docName + ".pdf";
                                    if (pRunGS.equals("Y")) {
                                        gsExec = pCmdGS + pDirOutput + docName + ".pdf " + pTempOutput + docName +
                                                ".pdf";
                                    }
                                    Tools.executeGS(gsExec);

                                    is = new ByteArrayInputStream(outputStream.toByteArray());
                                    if (!Tranform.convertToXML(is, pDirOutput + docName + ".xml", pXslTemplate)) {
                                        if (pUpdateDB.equals("Y")) {
                                            ValidationResult errResult = new ValidationResult();
                                            errResult.addError(ErrorCatalog.xmlCreationError());
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
                                if (!Tranform.convertToUBL(is, ublFile, pUblXsltPath)) {
                                    if (pUpdateDB.equals("Y")) {
                                        ValidationResult errResult = new ValidationResult();
                                        errResult.addError(ErrorCatalog.ublCreationError());
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
                                        if (!Tranform.embedPdfInUBL(ublFile, pdfFile, pdfFileName)) {
                                            log(LogCatalog.ublAttachmentError(docName));
                                        } else {
                                            log(LogCatalog.ublAttachmentSuccess(docName));
                                        }
                                    }

                                    Document ublDoc = Tranform.parseUBLFile(ublFile);
                                    ValidationResult validResult = pUBLValidator.validateUbl(ublDoc);

                                    // Populate UBL tables if enabled (before sending to PA)
                                    if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                        try {

                                            // Insert lifecycle event: CREATED
                                            InvoiceStatusCatalog.created().apply(dbHandler);

                                            // Insert header
                                            String numClient = Tools.getNodeString(pNumClient, (Element) element);
                                            if (dbHandler.insertUBLHeader(ublDoc,
                                                    null, null, null, null, numClient, InvoiceStatusCatalog.STATUS_ISSUED)) {
                                                // Insert lines
                                                dbHandler.insertUBLLines(ublDoc);

                                                // Insert VAT summary
                                                dbHandler.insertVATSummary(ublDoc);

                                                // Insert validation results
                                                dbHandler.insertValidationResults(
                                                        validResult);
                                            }
                                        } catch (Exception e) {
                                            log(LogCatalog.dbUblTablesFailed(docName));
                                            log(LogCatalog.dbUblTablesError(e.getMessage()));
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
                                                InvoiceStatusCatalog.validatedWithWarnings().apply(dbHandler);
                                            } catch (Exception e) {
                                                log(LogCatalog.dbUpdateFailed(e.getMessage()));
                                            }
                                        }

                                        // Check if only warnings (no errors)
                                        boolean hasOnlyWarnings = true;
                                        for (ValidationError err : validResult.getErrors()) {
                                            if (!"WARNING".equalsIgnoreCase(err.getSeverity().toUpperCase())) {
                                                hasOnlyWarnings = false;
                                                break;
                                            }
                                        }

                                        // Send to PA if F (force) mode and only warnings (not in validation-only mode)
                                        if (!pParamType.equals("UBL_VALIDATE") && "F".equalsIgnoreCase(pSendToPA)
                                                && hasOnlyWarnings) {
                                            log(LogCatalog.ublForceSendToPA(docName));

                                            // Update status before sending
                                            if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                try {
                                                    InvoiceStatusCatalog.sent().apply(dbHandler);
                                                } catch (Exception e) {
                                                    log(LogCatalog.dbUpdateFailed(e.getMessage()));
                                                }
                                            }

                                            if (!sendToPlatform(ublFile, docName)) {
                                                if (pUpdateDB.equals("Y")) {
                                                    ValidationResult errResult = new ValidationResult();
                                                    errResult.addError(ErrorCatalog.paSendError());
                                                    dbHandler.insertValidationResults(
                                                            errResult);
                                                }

                                                // Update status to ERROR
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        InvoiceStatusCatalog.errorSend().apply(dbHandler);
                                                    } catch (Exception e) {
                                                        log(LogCatalog.dbUpdateFailed(e.getMessage()));
                                                    }
                                                }
                                            } else {
                                                // Update status to DEPOSEE (deposited)
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        InvoiceStatusCatalog.deposited().apply(dbHandler);
                                                    } catch (Exception ex) {
                                                        log(LogCatalog.dbUpdateFailed(ex.getMessage()));
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        log(LogCatalog.ublValidationSuccess(typePiece, docName));

                                        // Update status to VALIDATED if UBL tables populated
                                        if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                            try {
                                                InvoiceStatusCatalog.validated().apply(dbHandler);
                                            } catch (Exception e) {
                                                log(LogCatalog.dbUpdateFailed(e.getMessage()));
                                            }
                                        }

                                        // Send to PA if enabled (Y or F mode) - not in validation-only mode
                                        if (!pParamType.equals("UBL_VALIDATE")
                                                && ("Y".equalsIgnoreCase(pSendToPA)
                                                        || "F".equalsIgnoreCase(pSendToPA))) {

                                            // Update status before sending
                                            if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                try {
                                                    InvoiceStatusCatalog.sent().apply(dbHandler);
                                                } catch (Exception ex2) {
                                                    log(LogCatalog.dbUpdateFailed(ex2.getMessage()));
                                                }
                                            }

                                            if (!sendToPlatform(ublFile, docName)) {
                                                if (pUpdateDB.equals("Y")) {
                                                    ValidationResult errResult = new ValidationResult();
                                                    errResult.addError(ErrorCatalog.paSendError());
                                                    dbHandler.insertValidationResults(
                                                            errResult);
                                                }

                                                // Update status to ERROR
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        InvoiceStatusCatalog.errorSend().apply(dbHandler);
                                                    } catch (Exception ex3) {
                                                        log(LogCatalog.dbUpdateFailed(ex3.getMessage()));
                                                    }
                                                }
                                            } else {
                                                // Update status to DEPOSEE (deposited)
                                                if ("Y".equalsIgnoreCase(pUpdateDB) && conn != null) {
                                                    try {
                                                        InvoiceStatusCatalog.deposited().apply(dbHandler);
                                                    } catch (Exception ex4) {
                                                        log(LogCatalog.dbUpdateFailed(ex4.getMessage()));
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
