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

import static custom.resources.Tools.decodePasswd;
import static custom.resources.Tools.encodePasswd;
import org.apache.commons.io.FileUtils;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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


    public static void updateUser(String configFile,String jobNumber,String jobName) throws Exception  {
        
      
            // register oracle driver
        try {
            File file = new File(configFile);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);
            
          
            // Initialisation des variables globales du fichier de propriétés
            Resource resource = resources.getResourceByName("global");
            String pURL = resource.getProperty("URL");
            String pSchemaSVM = resource.getProperty("schemaSVM");
            String pDBUser = resource.getProperty("DBUser");
            String pDBPasswd = decodePasswd(resource.getProperty("DBPassword"));

            
            Class.forName("oracle.jdbc.OracleDriver");
        
            // connect to oracle and login
            Connection conn = DriverManager.getConnection(pURL,pDBUser,pDBPasswd);
        
            // update to retrieve batch from PrintQueue
            String sql = "update "+pSchemaSVM+".F986110 SET JCUSER='EXPLOIT' WHERE JCJOBNBR=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1,Integer.parseInt(jobNumber));
            stmt.executeUpdate();
            stmt.close();

            String sql2 = "update "+pSchemaSVM+".F986110 SET JCUSER='EXPLOIT' WHERE JCJOBNBR=?";
            PreparedStatement stmt2 = conn.prepareStatement(sql2);
            String[] parts =jobName.split("_");        
            stmt2.setInt(1,Integer.parseInt(parts[2]));

            stmt2.executeUpdate();
            stmt2.close();
            conn.close();
        }
            catch (ClassNotFoundException | NumberFormatException | SQLException e)
            {
                e.printStackTrace();
            }
        
    }

    /* Insert du suivi des traitements dans une table de LOG */
    public static void insertLogSQL(String configFile,String paramTemplate, String paramFile, String paramType, String paramJobNumber,
            String Method, String Message) throws Exception  {
        
                         if (pUpdateDB.equals("Y")){
 
            // register oracle driver
        try {
            File file = new File(configFile);
            Serializer serializer = new Persister();
            Resources resources = serializer.read(Resources.class, file);
            
          
            // Initialisation des variables globales du fichier de propriétés
            Resource resource = resources.getResourceByName("global");
            String pURL = resource.getProperty("URL");
            String pSchema = resource.getProperty("schema");
            String pDBUser = resource.getProperty("DBUser");
            String pDBPassword = decodePasswd(resource.getProperty("DBPassword"));
            String pTableErr = resource.getProperty("tableLog")+"_LOG";
            
            // connect to oracle and login
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection(pURL,pDBUser,pDBPassword);
        
            String sql = "INSERT INTO "+ pSchema+"."+pTableErr+" (FEWDS1, FEUPMJ, FEUPMT, FEMODE, FETMPL, FEMETHOD, FEMESSAGE) VALUES (?,?,?,?,?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
//            DateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            DateFormat sdf2 = new SimpleDateFormat("YYYYDDD");
            DateFormat sdf3 = new SimpleDateFormat("HHmmss");
            java.util.Date date = new java.util.Date();
            stmt.setString(1,paramFile);
            stmt.setInt(2,Integer.parseInt(sdf2.format(date))-1900000);
            stmt.setInt(3,Integer.parseInt(sdf3.format(date)));
            stmt.setString(4,paramType);
            stmt.setString(5,paramTemplate);
            stmt.setString(6,Method);
            stmt.setString(7,Message);
     
            stmt.executeUpdate();
            stmt.close();
            conn.close();
        }
            catch (ClassNotFoundException | NumberFormatException | SQLException e)
            {
                e.printStackTrace();
            }
                         }
        
    }
    
    /* Renvoi un libellé en fonction du code erreur */
    private static String getMessage(){
        String message = "SUCCESSFUL";
        switch (errorCode){
                case 0 :  message = "SUCCESSFUL";
                        break;
                case 1 :  message = "FATAL ERROR";
                        break;
                case 2 :  message = "NO DATA SELECTED";
                        break;
                default : message = "SUCCESSFUL";
                        break;
        }
        return message;
        
    }
   
    /* Remise en forme des documents */
    public static void GenerateReport(String paramTemplate, String paramFile, String paramType, String paramJobNumber, 
            String paramConfig, boolean displayError) throws IOException, Exception{
        
    
            try {               
            
            /* Initialisation des valeurs de paramètres */
            Init(paramTemplate,paramConfig, paramFile);
            
            /* Log du début de traitement */
            insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"START","SUCCESSFUL");
            
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
                    insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"transformXSLToXML",transformResult.getErrorMessage());
                    insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
                    System.exit(1);
                }
                inputXML = tempXML;
            }
            
            BIPTransformResult<ByteArrayOutputStream> rtfConversionResult = BIPublisher.convertRTFXSL(pRtfTemplate);
            ByteArrayOutputStream xslOutStream = rtfConversionResult.getData();
            if (rtfConversionResult.hasError()) {
                insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"convertRTFXSL",rtfConversionResult.getErrorMessage());
                insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
                System.exit(1);               
            }
                       
            if (paramType.equals("SINGLE")) {
                 if (pDevMode.equals("Y")) {
                    TransformResult<Void> transformResult = Tranform.transformXSLToXML(inputXML,tempXML3,pDevXSL);
                    if (transformResult.hasError()) {
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"transformXSLToXML",transformResult.getErrorMessage());
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
                        System.exit(1);
                    }
                 }
                 else {
                    TransformResult<Void> transformResult = Tranform.transformXSLToXML(inputXML,tempXML2,pRoutage);
                    if (transformResult.hasError()) {
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"transformXSLToXML",transformResult.getErrorMessage());
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
                        System.exit(1);
                    }
                    TransformResult<Void> transformResultCopy = Tranform.transformXSLToXML(tempXML2,tempXML3,pCopy);
                    if (transformResultCopy.hasError()) {
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"transformXSLToXML",transformResultCopy.getErrorMessage());
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
                        System.exit(1);
                    }
                 }
                 
                runSingle(tempXML3 ,xslOutStream, paramFile + ".pdf");
                 if (errorCode.equals(1)) {
                    insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"runSingle",errorMessage);
                    insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
                    System.exit(1);
                 }
               
                // Copie des fichiers dans le répertoire E1
                FileUtils.copyDirectory(new File(pDirOutput),new File(pSingleOutput),false);
                
                // Vérification si le fichier n'est pas vide
                if (!FileUtils.readFileToString(new File(tempXML3),"UTF-8").contains("ID_DU_DOCUMENT"))
                    if (pUpdateDB.equals("Y")){
                        //updateUser(paramJobNumber,paramFile);
                        errorCode = 2;
                    }
                 
            } else {
                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = null;
                if (pDevMode.equals("Y")) {
                    TransformResult<Void> transformResult = Tranform.transformXSLToXML(inputXML,tempXML2,pDevXSL);
                    if (transformResult.hasError()) {
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"transformXSLToXML",transformResult.getErrorMessage());
                        insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
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

                if (paramType.equals("UBL") || paramType.equals("BOTH") || paramType.equals("UBL_VALIDATE")) {
                    ublValidator = new UBLValidator(pXsdPath, pSchematronPath);
                    
                    // Create shared TokenManager once for all tasks (real or mock)
                    // This avoids creating thousands of tokens for thousands of tasks
                    // Mock mode: Creates MockTokenManager to simulate token generation without PA connection
                    // Real mode: Creates real TokenManager that connects to PA API
                    if (!paramType.equals("UBL_VALIDATE")) {
                        
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
                    insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"runTasks",errorMessage);
                    insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
                    System.exit(1);
                 }
               // Copie des fichiers dans le répertoire d'envoi
                FileUtils.copyDirectory(new File(pDirOutput),new File(pBurstOutput),false);
                

             }
                  // Suppression fichier input
            FileUtils.forceDelete(new File(pDirInput + paramFile + ".xml"));
            insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",getMessage());
     
            
        } catch (IOException | NumberFormatException | ParserConfigurationException | SAXException e)
        {
           insertLogSQL(paramConfig,paramTemplate,paramFile,paramType,paramJobNumber,"END",e.getMessage());
           System.exit(1);
        } finally {
            // Suppression répertoire temporaire
            File rmdDir = new File(pProcessHome+"/"+paramTemplate+"/"+paramFile);
            FileUtils.forceDelete(rmdDir);
            
        } 
    }
    
    
    /* Fonction principale de l'application 
        Mode config = ouverture de l'application graphique
        Mode run = exécution en ligne de commande
        Mode password = creation des password cryptés
        Mode updUser = mise à jour de l'utilisateur E1 dans les travaux soumis
    */
    public static void main(String[] args) throws Exception{
               
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
            String paramType = args[4];
            String paramJobNumber = args[5];
      
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
