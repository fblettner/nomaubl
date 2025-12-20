package custom.resources;

import oracle.xdo.XDOException;
import oracle.xdo.template.FOProcessor;
import oracle.xdo.template.RTFProcessor;

import java.io.*;

public class BIPublisher {
    
        /* Generic result class for transformation operations */
    public static class BIPTransformResult<T> {
        private final T data;
        private final Integer errorCode;
        private final String errorMessage;

        public BIPTransformResult(T data, Integer errorCode, String errorMessage) {
            this.data = data;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public T getData() {
            return data;
        }

        public Integer getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean hasError() {
            return errorCode != null && errorCode != 0;
        }
        
        public boolean isSuccess() {
            return errorCode == null || errorCode == 0;
        }
    }

    /* Conversion d'un template RTF en XSL */
    public static BIPTransformResult<ByteArrayOutputStream> convertRTFXSL(String inputRTF) {

        ByteArrayOutputStream xslOutStream = new ByteArrayOutputStream();
        try {
            FileInputStream fIs = new FileInputStream(inputRTF); // input RTF template
            RTFProcessor rtfProcessor = new RTFProcessor(fIs);
            rtfProcessor.setOutput(xslOutStream);
            rtfProcessor.process();
            return new BIPTransformResult<>(xslOutStream, 0, null);
        } catch (FileNotFoundException | XDOException e) {
            return new BIPTransformResult<>(xslOutStream, 1, e.getMessage());
        }
    }

    // Mise en forme PDF via les API BI Publisher
    public static Boolean convertToPDF(InputStream xmlStream, String outputPDF, ByteArrayOutputStream xslOutStream,
            String xdoConfig, String setLocale) {
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
                processor.setConfig(xdoConfig);
                processor.setLocale(setLocale);
                // Traitement
                processor.generate();
            }

        } catch (IOException | XDOException e) {
            return false;
        }
        return true;
    }    
}
