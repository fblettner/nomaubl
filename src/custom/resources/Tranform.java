package custom.resources;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.Base64;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;

public class Tranform {

    /* Generic result class for transformation operations */
    public static class TransformResult<T> {
        private final T data;
        private final Integer errorCode;
        private final String errorMessage;

        public TransformResult(T data, Integer errorCode, String errorMessage) {
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


     /* Application d'une transformation XSL sur un document XML */
    public static TransformResult<Void> transformXSLToXML(String inputXML, String outputXML, String templateXSL){
        
        try
        {
            // Use the factory to create a template containing the xsl file
            TransformerFactory factory = TransformerFactory.newInstance();
            
            Source xml = new StreamSource(new FileInputStream(inputXML));
            Source xsl = new StreamSource("file:" + templateXSL);
            Templates template = factory.newTemplates(xsl);
            Transformer xformer = template.newTransformer();
            
            FileOutputStream fos = new FileOutputStream(outputXML);
            Result result = new StreamResult(fos);
            
            xformer.transform(xml, result);
            fos.close();
            return new TransformResult<>(null, 0, null);
        }
        catch (IOException | TransformerException e)
        {
            return new TransformResult<>(null, 1, e.getMessage());
        }
    }

    public static Boolean convertToXML(InputStream xmlStream, String outputXML, String xslTemplate) {
        try {
            // Use the factory to create a template containing the xsl file
            TransformerFactory factory = TransformerFactory.newInstance();

            Source xml = new StreamSource(xmlStream);
            Source xsl = new StreamSource("file:" + xslTemplate);
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

    public static boolean convertToUBL(InputStream invoiceXmlStream, String outputUblFile, String ublXsltPath) {
        try {
            // Use Saxon for XSLT 2.0 support
            TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
            Source xml = new StreamSource(invoiceXmlStream);
            Source xsl = new StreamSource("file:" + ublXsltPath);
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

    public static Document parseUBLFile(String ublFilePath) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new File(ublFilePath));
    }

    /**
     * Embeds a PDF file as base64 in the UBL XML document
     * 
     * @param ublFilePath Path to the UBL XML file
     * @param pdfFilePath Path to the PDF file to embed
     * @param pdfFileName Filename to use in the attachment
     * @return true if successful, false otherwise
     */
    public static boolean embedPdfInUBL(String ublFilePath, String pdfFilePath, String pdfFileName) {
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


}
