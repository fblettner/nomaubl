/*
 * Copyright (c) 2025 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package nomaubl;

import Frames.DarkTheme;
import Frames.MainModern;
import java.io.*;
import org.w3c.dom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.CompletionService;
import oracle.xdo.template.FOProcessor;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import custom.resources.*;
import custom.resources.BIPublisher.BIPTransformResult;
import custom.resources.Tranform.TransformResult;
import custom.ubl.CustomUBL;
import custom.ubl.MockTokenManager;
import custom.ubl.UBLValidator;
import custom.ubl.TokenManager;
import custom.ubl.RuntimeLogHandler;
import custom.ubl.RuntimeLogCatalog;
import custom.ubl.JDEUserUpdater;
import custom.ubl.ProcessingType;

import static custom.resources.Tools.encodePasswd;
import org.apache.commons.io.FileUtils;
import javax.xml.parsers.ParserConfigurationException;
import oracle.xdo.XDOException;
import org.xml.sax.SAXException;


public class ScheduleUBL {
    
    private static String pAppHome;
    private static String pProcessHome;
    private static String pXdoConfig;
    private static String pDirInput;
    private static String pRtfTemplate;
    private static String pBurstKey;
    private static String pDirOutput;
    private static String pTempOutput;
    private static String pRunGS;
    private static String pCmdGS;
    private static String pTransform;
    private static String pTransformYN;
    private static String pRoutage;
    private static String pCopy;
    private final static String TEMPLATE = "%TEMPLATE%";
    private final static String FILE_NAME = "%FILE_NAME%";
    private final static String APP_HOME = "%APP_HOME%";
    private final static String PROCESS_HOME = "%PROCESS_HOME%";
    private static String templateName;
    private static String fileName;
    private static String pBurstOutput;
    private static String pSingleOutput;
    private static String pNumProc;
    private static String pUpdateDB;
    private static String pSetLocale;
    private static String pDevMode;
    private static String pDevXSL;
    private static String errorMessage = " ";
    private static Integer errorCode = 0;
    private static String pXsdPath;
    private static String pSchematronPath;
    private static String paApiBaseUrl;
    private static String paApiLoginEndpoint;
    private static String paApiUsername;
    private static String paApiPassword;
    private static int paApiTimeout;
                        
    private static String useMock;
    private static String mockBehavior;
    private static String pAttachment;
    
    /* Remplacement des variables dans les emplacements de fichier */
    private static String replaceConstValue (String inputStr) {
        String replaceStr = inputStr.replace(APP_HOME,pAppHome);
        replaceStr = replaceStr.replace(PROCESS_HOME,pProcessHome);
        replaceStr = replaceStr.replace(TEMPLATE,templateName);
        replaceStr = replaceStr.replace(FILE_NAME,fileName);
        return replaceStr;
    }
    
    /* Initialisation des variables */
    private static void Init(String inputTemplate, String configFile, String inputFile) {
        try {
            File file = new File(configFile);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);
            
            templateName = inputTemplate;
            fileName = inputFile;
            
            // Initialisation des variables globales du fichier de propriétés
            Resource resource = resources.getResourceByName("global");
            pAppHome = resource.getProperty("appHome");
            pProcessHome = resource.getProperty("processHome");
            pXdoConfig = resource.getProperty("xdo");
            pDirInput = replaceConstValue(resource.getProperty("dirInput"));
            pDirOutput = replaceConstValue(resource.getProperty("dirOutput"));
            pTempOutput = replaceConstValue(resource.getProperty("tempOutput"));
            pRunGS = resource.getProperty("runGS");
            pCmdGS = resource.getProperty("cmdGS");
            pRoutage = replaceConstValue(resource.getProperty("routageXSL"));
            pCopy = replaceConstValue(resource.getProperty("copyXSL"));
            pBurstOutput = resource.getProperty("burstOutput");
            pSingleOutput = resource.getProperty("singleOutput");
            pUpdateDB = resource.getProperty("updateDB");
            pSetLocale = resource.getProperty("setLocale");
            pDevMode = resource.getProperty("devModeYN");
            pDevXSL = replaceConstValue(resource.getProperty("devXSL"));
            pXsdPath = replaceConstValue(resource.getProperty("ublXsdPath"));
            pSchematronPath = replaceConstValue(resource.getProperty("ublSchematronPath"));
            paApiBaseUrl = resource.getProperty("paApiBaseUrl");
            paApiLoginEndpoint = resource.getProperty("paApiLoginEndpoint");
            paApiUsername = resource.getProperty("paApiUsername");
            paApiPassword = resource.getProperty("paApiPassword");
            String timeout = resource.getProperty("paApiTimeout");
            paApiTimeout = (timeout != null) ? Integer.parseInt(timeout) : 30000;
                            
            useMock = resource.getProperty("paUseMock");
            mockBehavior = resource.getProperty("paMockBehavior");
 
            // Création des répertoires
            FileUtils.forceMkdir(new File(pDirOutput));
            FileUtils.forceMkdir(new File(pTempOutput));
            
            // Initialisation des variables spécifiques à un template
            resource = resources.getResourceByName(templateName);
            pBurstKey = resource.getProperty("burstKey");
            pRtfTemplate = replaceConstValue(resource.getProperty("rtf"));
            pTransform = replaceConstValue(resource.getProperty("transform"));
            pTransformYN = resource.getProperty("transformYN");
            pNumProc = resource.getProperty("numProc");
            pAttachment = resource.getProperty("attachment");

        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

    }

    /* Exécution en parallèle des remises en forme de documents */
    public static void runTasks(final ExecutorService executor, List<Callable<Integer>> taches){
        
        //Le service de terminaison
        CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);
        
        //une liste de Future pour récupérer les résultats
        List<Future<Integer>> futures = new ArrayList<>();
        
//        Integer res = null;
        try {
            //On soumet toutes les tâches à l'executor
            for(Callable<Integer> t : taches){
                futures.add(completionService.submit(t));
            }
            
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        catch(RuntimeException e){
            errorMessage = e.getMessage();
            errorCode = 1;            
        }
        finally {
            executor.shutdown();
        }
    }
    
    /* Remise en forme d'un document */
    public static void runSingle(String inputXML, ByteArrayOutputStream outputStream,String outputPDF)
    {
        try
        {
            FOProcessor processor = new FOProcessor();
            processor.setData(inputXML); //input XML
            InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
            processor.setTemplate(is);  //input XSL template
            processor.setOutput(pTempOutput + outputPDF ); // output PDF File
            
            // Set output format (for PDF generation)
            processor.setOutputFormat(FOProcessor.FORMAT_PDF);
            processor.setConfig(pXdoConfig);
            processor.setLocale(pSetLocale);
            
            // Start processing
            processor.generate();
            
            String gsExec = "cp " + pTempOutput + outputPDF + " " + pDirOutput + outputPDF ;
            if (pRunGS.equalsIgnoreCase("Y")) { 
                gsExec = pCmdGS + pDirOutput + outputPDF + " " + pTempOutput + outputPDF;
            }

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(gsExec);
            proc.waitFor();
        }
        catch (IOException | InterruptedException | XDOException e)
        {
            errorMessage = e.getMessage();
            errorCode = 1;
        }
        
    }


    /**
     * Update JDE user in submitted jobs
     * @deprecated Use JDEUserUpdater class instead
     */
    @Deprecated
    public static void updateUser(String configFile, String jobNumber, String jobName) throws Exception {
        JDEUserUpdater updater = new JDEUserUpdater(configFile);
        JDEUserUpdater.UpdateResult result = updater.updateUser(jobNumber, jobName);
        if (result.hasError()) {
            System.err.println("Failed to update user: " + result.getErrorMessage());
        }
    }

    /* Insert du suivi des traitements dans une table de LOG */
    @Deprecated
    public static void insertLogSQL(String configFile,String paramTemplate, String paramFile, ProcessingType paramType, String paramJobNumber,
            String Method, String Message) throws Exception  {
        // Deprecated - Use RuntimeLogHandler instead
        RuntimeLogHandler logHandler = new RuntimeLogHandler(configFile, paramTemplate, paramFile, paramType);
        RuntimeLogHandler.LogResult result = logHandler.insertLog(Method, Message);
        if (result.hasError()) {
            System.err.println("Runtime log error: " + result.getErrorMessage());
        }
    }
      
    /* Remise en forme des documents */
    public static void GenerateReport(String paramTemplate, String paramFile, ProcessingType paramType, String paramJobNumber, 
            String paramConfig, boolean displayError) throws IOException, Exception{
        
            // Create runtime log handler for this execution
            RuntimeLogHandler logHandler = new RuntimeLogHandler(paramConfig, paramTemplate, paramFile, paramType);

            try {               
            
            /* Initialisation des valeurs de paramètres */
            Init(paramTemplate,paramConfig, paramFile);
            
            /* Log du début de traitement */
            RuntimeLogHandler.LogResult logResult = logHandler.logStart();
            if (logResult.hasError()) {
                System.err.println("Failed to log START: " + logResult.getErrorMessage());
            }
            
            /* Initialisation du fichier d'entrée */
            String inputXML = pDirInput + paramFile + ".xml";
            
            /* Initialisation des fichiers temporaires */
            String tempXML = pTempOutput + paramFile + ".xml";
            String tempXML2 = pTempOutput + paramFile + "_2.xml";
            String tempXML3 = pTempOutput + paramFile + "_3.xml";

            /* Application d'une transformation XSLT au début du traitement */
            if (pTransformYN.equals("Y")) {
                TransformResult<Void> transformResult = Tranform.transformXSLToXML(inputXML,tempXML, pTransform);
                
                if (transformResult.hasError()) {
                    logHandler.logError(RuntimeLogCatalog.METHOD_TRANSFORM_XSL, transformResult.getErrorMessage());
                    logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                    System.exit(1);
                }
                inputXML = tempXML;
            }
            
            ByteArrayOutputStream xslOutStream = null;
            
            // Conversion RTF uniquement si nécessaire pour BURST, BOTH ou création d'attachments
            if (paramType.involvesPDF() || (pAttachment != null && pAttachment.equals("create"))) {
                BIPTransformResult<ByteArrayOutputStream> rtfConversionResult = BIPublisher.convertRTFXSL(pRtfTemplate);
                xslOutStream = rtfConversionResult.getData();
                if (rtfConversionResult.hasError()) {
                    logHandler.logError(RuntimeLogCatalog.METHOD_CONVERT_RTF, rtfConversionResult.getErrorMessage());
                    logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                    System.exit(1);               
                }
            }
                       
            if (paramType == ProcessingType.SINGLE) {
                 if (pDevMode.equals("Y")) {
                    TransformResult<Void> transformResult = Tranform.transformXSLToXML(inputXML,tempXML3,pDevXSL);
                    if (transformResult.hasError()) {
                        logHandler.logError(RuntimeLogCatalog.METHOD_TRANSFORM_XSL, transformResult.getErrorMessage());
                        logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                        System.exit(1);
                    }
                 }
                 else {
                    TransformResult<Void> transformResult = Tranform.transformXSLToXML(inputXML,tempXML2,pRoutage);
                    if (transformResult.hasError()) {
                        logHandler.logError(RuntimeLogCatalog.METHOD_TRANSFORM_XSL, transformResult.getErrorMessage());
                        logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                        System.exit(1);
                    }
                    TransformResult<Void> transformResultCopy = Tranform.transformXSLToXML(tempXML2,tempXML3,pCopy);
                    if (transformResultCopy.hasError()) {
                        logHandler.logError(RuntimeLogCatalog.METHOD_TRANSFORM_XSL, transformResultCopy.getErrorMessage());
                        logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                        System.exit(1);
                    }
                 }
                 
                runSingle(tempXML3 ,xslOutStream, paramFile + ".pdf");
                 if (errorCode.equals(1)) {
                    logHandler.logError(RuntimeLogCatalog.METHOD_RUN_SINGLE, errorMessage);
                    logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                    System.exit(1);
                 }
               
                // Copie des fichiers dans le répertoire E1
                FileUtils.copyDirectory(new File(pDirOutput),new File(pSingleOutput),false);
                
                // Vérification si le fichier n'est pas vide
                if (!FileUtils.readFileToString(new File(tempXML3),"UTF-8").contains("ID_DU_DOCUMENT"))
                    if (pUpdateDB.equals("Y")){
                        logHandler.logEnd(RuntimeLogCatalog.STATUS_NO_DATA);
                    }
                 
            } else {
                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = null;
                if (pDevMode.equals("Y")) {
                    TransformResult<Void> transformResult = Tranform.transformXSLToXML(inputXML,tempXML2,pDevXSL);
                    if (transformResult.hasError()) {
                        logHandler.logError(RuntimeLogCatalog.METHOD_TRANSFORM_XSL, transformResult.getErrorMessage());
                        logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                        System.exit(1);
                    }
                    doc = builder.parse("file:" + tempXML2);
                } else {
                    doc = builder.parse("file:" + inputXML);                    
                }
        
                List<Callable<Integer>> tasks = new ArrayList<>();
        
                NodeList list = doc.getElementsByTagName(pBurstKey);
        
                int processorCount = Integer.parseInt(pNumProc);
                int numberOfInvoice = list.getLength();
                int numberOfSplit = numberOfInvoice / 200;
        
                if (numberOfSplit < 4)
                    numberOfSplit = 4;
        
                int splitInvoiceNumber = numberOfInvoice / numberOfSplit;
                int splitMod = numberOfInvoice % numberOfSplit;

                UBLValidator ublValidator = null;
                TokenManager tokenManager = null;

                if (paramType.involvesUBL()) {
                    ublValidator = new UBLValidator(pXsdPath, pSchematronPath);
                    
                    // Create shared TokenManager once for all tasks (real or mock)
                    // This avoids creating thousands of tokens for thousands of tasks
                    // Mock mode: Creates MockTokenManager to simulate token generation without PA connection
                    // Real mode: Creates real TokenManager that connects to PA API
                    if (paramType.shouldSendToPA()) {
                        
                        if ("Y".equalsIgnoreCase(useMock)) {
                            // Mock mode: Create MockTokenManager to simulate authentication
                            MockTokenManager.TokenBehavior behavior = MockTokenManager.TokenBehavior.ALWAYS_SUCCESS;
                            if (mockBehavior != null) {
                                try {
                                    behavior = MockTokenManager.TokenBehavior.valueOf(mockBehavior.toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    // Use default behavior
                                }
                            }
                            tokenManager = new MockTokenManager(
                                paApiBaseUrl,
                                paApiLoginEndpoint,
                                paApiUsername,
                                paApiPassword,
                                paApiTimeout,
                                behavior
                            );
                            
                            // Pre-fetch token to test authentication flow
                            String initialToken = tokenManager.getToken();
                            if (initialToken == null) {
                                throw new Exception("Mock authentication failed - check mock behavior");
                            }
                        } else {
                            // Real API mode: Create real TokenManager
                            tokenManager = new TokenManager(
                                paApiBaseUrl,
                                paApiLoginEndpoint,
                                paApiUsername,
                                paApiPassword,
                                paApiTimeout
                            );
                            
                            // Pre-fetch token to fail early if credentials are wrong
                            String initialToken = tokenManager.getToken();
                            if (initialToken == null) {
                                throw new Exception("Failed to authenticate with PA API - check credentials");
                            }
                        }
                    }
                }
                
                for (int i=0; i<numberOfSplit; i++){
                    tasks.add(new CustomUBL(i*splitInvoiceNumber,splitInvoiceNumber+(splitInvoiceNumber*i),list,xslOutStream,paramTemplate,paramFile,paramConfig, paramType, ublValidator, tokenManager, displayError));
                }
                tasks.add(new CustomUBL(numberOfInvoice-splitMod,numberOfInvoice,list,xslOutStream,paramTemplate,paramFile, paramConfig, paramType, ublValidator, tokenManager, displayError));
        
                ExecutorService execute = Executors.newFixedThreadPool(processorCount);
                runTasks(execute, tasks);
                if (errorCode.equals(1)) {
                    logHandler.logError(RuntimeLogCatalog.METHOD_RUN_TASKS, errorMessage);
                    logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR);
                    System.exit(1);
                 }
               // Copie des fichiers dans le répertoire d'envoi
                FileUtils.copyDirectory(new File(pDirOutput),new File(pBurstOutput),false);
                

             }
            // Suppression fichier input
            FileUtils.forceDelete(new File(pDirInput + paramFile + ".xml"));
            logHandler.logEnd(RuntimeLogCatalog.STATUS_SUCCESSFUL);
     
            
        } catch (IOException | NumberFormatException | ParserConfigurationException | SAXException e)
        {
           logHandler.logEnd(RuntimeLogCatalog.STATUS_FATAL_ERROR + ": " + e.getMessage());
           System.exit(1);
        } finally {
            // Suppression répertoire temporaire
            File rmdDir = new File(pProcessHome+"/"+paramTemplate+"/"+paramFile);
            FileUtils.forceDelete(rmdDir);
            
        } 
    }
    
    
    /* Affiche l'aide de l'application */
    private static void displayHelp() {
        System.out.println("=================================================================");
        System.out.println("NomaUBL - UBL Document Processing Application");
        System.out.println("=================================================================");
        System.out.println("\nUsage: java -jar nomaubl.jar [MODE] [PARAMETERS]\n");
        System.out.println("Available modes:\n");
        System.out.println("  -help, --help, -h");
        System.out.println("      Display this help message\n");
        System.out.println("  -config <configFile>");
        System.out.println("      Open the graphical user interface");
        System.out.println("      Parameters:");
        System.out.println("        configFile: Path to the configuration file\n");
        System.out.println("  -run <configFile> <template> <fileName> <type> <jobNumber>");
        System.out.println("      Execute document processing in command line mode");
        System.out.println("      Parameters:");
        System.out.println("        configFile: Path to the configuration file");
        System.out.println("        template:   Template name to use");
        System.out.println("        fileName:   Input file name (without extension)");
        System.out.println("        type:       Processing type (SINGLE, BURST, UBL, BOTH, UBL_VALIDATE)");
        System.out.println("        jobNumber:  Job number for tracking\n");
        System.out.println("  -password <password>");
        System.out.println("      Encode a password for storage in configuration");
        System.out.println("      Parameters:");
        System.out.println("        password: The password to encode\n");
        System.out.println("  -updUser <configFile> <jobNumber> <fileName>");
        System.out.println("      Update E1 user in submitted jobs");
        System.out.println("      Parameters:");
        System.out.println("        configFile: Path to the configuration file");
        System.out.println("        jobNumber:  Job number to update");
        System.out.println("        fileName:   File name associated with the job\n");
        System.out.println("Examples:");
        System.out.println("  java -jar nomaubl.jar -help");
        System.out.println("  java -jar nomaubl.jar -config ./config/config.properties");
        System.out.println("  java -jar nomaubl.jar -run ./config/config.properties invoice doc_123 SINGLE 1");
        System.out.println("  java -jar nomaubl.jar -password mySecretPass");
        System.out.println("=================================================================");
    }
    
    /* Fonction principale de l'application 
        Mode config = ouverture de l'application graphique
        Mode run = exécution en ligne de commande
        Mode password = creation des password cryptés
        Mode updUser = mise à jour de l'utilisateur E1 dans les travaux soumis
    */
    public static void main(String[] args) throws Exception{
        
        if (args.length == 0 || args[0].equals("-help") || args[0].equals("--help") || args[0].equals("-h")) {
            displayHelp();
            System.exit(0);
        }
               
        String paramMode = args[0];
        String paramConfig = args[1];
        
        if (paramMode.equals("-config")) {
                DarkTheme.apply();
                MainModern MainFrame = new MainModern();
                MainFrame.MainInit(paramConfig);
                MainFrame.setVisible(true);
            } 
        if (paramMode.equals("-run")) {
            String paramTemplate = args[2];
            String paramFile = args[3];
            String paramTypeString = args[4];
            String paramJobNumber = args[5];
            
            // Parse processing type from string
            ProcessingType paramType;
            try {
                paramType = ProcessingType.fromString(paramTypeString);
            } catch (IllegalArgumentException e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
                return;
            }
      
            // Init(paramTemplate,paramConfig, paramFile);
            GenerateReport(paramTemplate,paramFile,paramType,paramJobNumber,paramConfig, false);
            
            System.exit(errorCode);
            }
        if (paramMode.equals("-password")){
            System.out.println(encodePasswd(args[1]));
        }
        if (paramMode.equals("-updUser")){
            String paramJobNumber = args[2];
            String paramFile = args[3];
            updateUser(paramConfig,paramJobNumber,paramFile);
        }

    }
}
