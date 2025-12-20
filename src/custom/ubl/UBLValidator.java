package custom.ubl;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import net.sf.saxon.TransformerFactoryImpl;

import java.io.*;
import java.util.*;

/**
 * UBL multi-level validator:
 * - XSD
 * - Schematron (EN16931, CIUS-FR) via ISO + Saxon
 */
public class UBLValidator {

    private final Schema ublSchema;
    private final Map<String, File> schematronXsls = new HashMap<>();

    // =========================
    // Constructor
    // =========================
    public UBLValidator(String xsdPath, String schematronPath) throws Exception {

        // XSD
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.ublSchema = sf.newSchema(new File(xsdPath));

        // Schematrons (.sch → compiled .xsl)
        schematronXsls.put(
                "EN16931",
                compileSchematron(new File(schematronPath + "/EN16931-UBL-validation-preprocessed.sch"), schematronPath));

        schematronXsls.put(
                "CIUS-FR",
                compileSchematron(new File(schematronPath + "/20251114_BR-FR-Flux2-Schematron-UBL_V1.2.0.sch"), schematronPath));
    }

    // =========================
    // Public API
    // =========================
    public ValidationResult validateUbl(Document ublDocument) {
        ValidationResult result = new ValidationResult();

        // Level 1: XSD
        result.merge(validateXSD(ublDocument));
        if (!result.isValid()) {
            return result;
        }

        // Level 2: EN16931
        result.merge(validateSchematron(ublDocument, "EN16931"));

        // Level 3: CIUS-FR
        result.merge(validateSchematron(ublDocument, "CIUS-FR"));

        return result;
    }

    // =========================
    // XSD validation
    // =========================
    public ValidationResult validateXSD(Document ublDocument) {
        ValidationResult result = new ValidationResult();

        try {
            Validator validator = ublSchema.newValidator();
            ValidationErrorHandler handler = new ValidationErrorHandler();
            validator.setErrorHandler(handler);
            validator.validate(new DOMSource(ublDocument));
            result.addErrors(handler.getErrors());

        } catch (Exception e) {
            result.addError(new ValidationError(
                    "XSD",
                    "FATAL",
                    "Erreur validation XSD: " + e.getMessage(),
                    null));
        }

        return result;
    }

    // =========================
    // Schematron validation
    // =========================
    public ValidationResult validateSchematron(Document ublDocument, String profile) {
        ValidationResult result = new ValidationResult();
        File xsl = schematronXsls.get(profile);

        if (xsl == null) {
            return result;
        }

        try {
            TransformerFactory tf = new TransformerFactoryImpl();
            Transformer transformer = tf.newTransformer(new StreamSource(xsl));

            ByteArrayOutputStream svrlOut = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(ublDocument), new StreamResult(svrlOut));

            Document svrlDoc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(svrlOut.toByteArray()));

            NodeList failedAsserts = svrlDoc.getElementsByTagNameNS(
                    "http://purl.oclc.org/dsdl/svrl",
                    "failed-assert");

            for (int i = 0; i < failedAsserts.getLength(); i++) {
                Element fa = (Element) failedAsserts.item(i);

                String flag = fa.getAttribute("flag");
                String ruleId = fa.getAttribute("id");

                // Le message du Schematron est déjà normatif et contient souvent l'ID
                String message = fa.getTextContent().trim();

                ValidationError error = new ValidationError(
                        profile,
                        (flag == null || flag.isEmpty()) ? "error" : flag,
                        message,
                        ruleId);
                
                result.addError(error);
            }

        } catch (Exception e) {
            result.addError(new ValidationError(
                    profile,
                    "FATAL",
                    "Erreur Schematron: " + e.getMessage(),
                    null));
        }

        return result;
    }

    // =========================
    // Compile .sch → .xsl (ISO)
    // =========================
    private File compileSchematron(File schFile, String schematronPath) throws Exception {

        TransformerFactory tf = new TransformerFactoryImpl();

        ByteArrayOutputStream step1 = new ByteArrayOutputStream();
        tf.newTransformer(new StreamSource(new File(schematronPath + "/iso_dsdl_include.xsl")))
                .transform(new StreamSource(schFile), new StreamResult(step1));

        ByteArrayOutputStream step2 = new ByteArrayOutputStream();
        tf.newTransformer(new StreamSource(new File(schematronPath + "/iso_abstract_expand.xsl"))).transform(
                new StreamSource(new ByteArrayInputStream(step1.toByteArray())),
                new StreamResult(step2));

        File xslFile = File.createTempFile("schematron-", ".xsl");
        tf.newTransformer(new StreamSource(new File(schematronPath + "/iso_svrl_for_xslt2.xsl"))).transform(
                new StreamSource(new ByteArrayInputStream(step2.toByteArray())),
                new StreamResult(xslFile));

        return xslFile;
    }
}