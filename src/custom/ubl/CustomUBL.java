/*
 * Copyright (c) 2018 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package custom.ubl;

import java.io.*;
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
    private IPlatformApiClient pPlatformApiClient;

    /**
     * Log using a LogEntry from LogCatalog
     * 
     * @param entry LogEntry to log
     */
    private void log(LogCatalog.LogEntry entry) {
        entry.print(displayError);
    }

    /**
     * Helper method to update invoice status with database error handling
     */
    private void updateStatus(InvoiceStatusCatalog.StatusTransition status, UBLDatabaseHandler dbHandler,
            Connection conn) {
        if (!"Y".equalsIgnoreCase(pUpdateDB) || conn == null) {
            return;
        }
        try {
            status.apply(dbHandler);
        } catch (Exception e) {
            log(LogCatalog.dbUpdateFailed(e.getMessage()));
        }
    }

    /**
     * Handle validation failure - log errors/warnings and potentially send to PA in
     * force mode
     */
    private void handleValidationFailure(ValidationResult validResult, String docName, String ublFile,
            UBLDatabaseHandler dbHandler, Connection conn) {
        // Display all errors/warnings
        for (ValidationError e : validResult.getErrors()) {
            String ruleId = e.getRuleId() != null ? e.getRuleId() : "UNDEFINED";
            log(LogCatalog.generic(e.getSeverity(), e.getSource(), ruleId, e.getMessage()));
        }

        // Update status to VALIDATED (with warnings)
        updateStatus(InvoiceStatusCatalog.validatedWithWarnings(), dbHandler, conn);

        // Check if only warnings (no errors)
        boolean hasOnlyWarnings = validResult.getErrors().stream()
                .allMatch(err -> "WARNING".equalsIgnoreCase(err.getSeverity()));

        // Send to PA if F (force) mode and only warnings (not in validation-only mode)
        if (!pParamType.equals("UBL_VALIDATE") && "F".equalsIgnoreCase(pSendToPA) && hasOnlyWarnings) {
            log(LogCatalog.ublForceSendToPA(docName));
            sendToPlatformAPI(ublFile, docName, dbHandler, conn);
        }
    }

    /**
     * Handle validation success - log success and send to PA if enabled
     */
    private void handleValidationSuccess(String typePiece, String docName, String ublFile,
            UBLDatabaseHandler dbHandler, Connection conn) {
        log(LogCatalog.ublValidationSuccess(typePiece, docName));

        // Update status to VALIDATED
        updateStatus(InvoiceStatusCatalog.validated(), dbHandler, conn);

        // Send to PA if enabled (Y or F mode) - not in validation-only mode
        boolean shouldSendToPA = !pParamType.equals("UBL_VALIDATE")
                && ("Y".equalsIgnoreCase(pSendToPA) || "F".equalsIgnoreCase(pSendToPA));

        if (shouldSendToPA) {
            sendToPlatformAPI(ublFile, docName, dbHandler, conn);
        }
    }

    /**
     * Helper method to send document to Platform API and handle status updates
     */
    private void sendToPlatformAPI(String ublFile, String docName, UBLDatabaseHandler dbHandler, Connection conn) {
        // Update status before sending
        updateStatus(InvoiceStatusCatalog.sent(), dbHandler, conn);

        boolean sendSuccess = pPlatformApiClient.sendDocument(ublFile, docName);

        if (!sendSuccess) {
            // Log error in validation results
            if ("Y".equalsIgnoreCase(pUpdateDB)) {
                ValidationResult errResult = new ValidationResult();
                errResult.addError(ErrorCatalog.paSendError());
                dbHandler.insertValidationResults(errResult);
            }
            // Update status to ERROR
            updateStatus(InvoiceStatusCatalog.errorSent(), dbHandler, conn);
        } else {
            // Update status to DEPOSEE (deposited)
            updateStatus(InvoiceStatusCatalog.deposited(), dbHandler, conn);
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

            // Load PA API configuration from global template
            pSendToPA = resource.getProperty("sendToPA");
            String paMode = resource.getProperty("paMode");
            String paApiBaseUrl = resource.getProperty("paApiBaseUrl");
            String paApiImportEndpoint = resource.getProperty("paApiImportEndpoint");
            String timeout = resource.getProperty("paApiTimeout");
            int paApiTimeout = (timeout != null) ? Integer.parseInt(timeout) : 30000;

            // Check if mock mode is enabled
            String useMock = resource.getProperty("paUseMock");
            String mockBehavior = resource.getProperty("paMockBehavior");

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

            // Initialize PA API client (real or mock)
            if ("Y".equalsIgnoreCase(useMock)) {
                MockPlatformApiClient.MockBehavior behavior = MockPlatformApiClient.MockBehavior.ALWAYS_SUCCESS;
                if (mockBehavior != null) {
                    try {
                        behavior = MockPlatformApiClient.MockBehavior.valueOf(mockBehavior.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Use default behavior
                    }
                }
                pPlatformApiClient = new MockPlatformApiClient(
                        paMode,
                        paApiBaseUrl,
                        paApiImportEndpoint,
                        paApiTimeout,
                        displayError,
                        behavior);
            } else {
                pPlatformApiClient = new PlatformApiClient(
                        paMode,
                        paApiBaseUrl,
                        paApiImportEndpoint,
                        paApiTimeout,
                        pTokenManager,
                        displayError);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // System.exit(1);
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
                                                    null, null, null, null, numClient,
                                                    InvoiceStatusCatalog.STATUS_CREATED,
                                                    InvoiceStatusCatalog.MSG_CREATED)) {
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

                                    // Process validation results
                                    if (!validResult.isValid()) {
                                        handleValidationFailure(validResult, docName, ublFile, dbHandler, conn);
                                    } else {
                                        handleValidationSuccess(typePiece, docName, ublFile, dbHandler, conn);
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
        if ("Y".equalsIgnoreCase(pUpdateDB))
            conn.close();
        return 0;
    }



}
