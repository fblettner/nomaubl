package custom.ubl;

import java.util.ArrayList;
import java.util.List;

public class ValidationErrorHandler implements org.xml.sax.ErrorHandler {
    
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationError> warnings = new ArrayList<>();
    
    @Override
    public void warning(org.xml.sax.SAXParseException e) {
        warnings.add(new ValidationError(
            "XSD",
            "warning",
            e.getMessage(),
            "Line " + e.getLineNumber() + ", Column " + e.getColumnNumber(),
            null
        ));
    }
    
    @Override
    public void error(org.xml.sax.SAXParseException e) {
        errors.add(new ValidationError(
            "XSD",
            "error",
            e.getMessage(),
            "Line " + e.getLineNumber() + ", Column " + e.getColumnNumber(),
            null
        ));
    }
    
    @Override
    public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
        errors.add(new ValidationError(
            "XSD",
            "fatal",
            e.getMessage(),
            "Line " + e.getLineNumber() + ", Column " + e.getColumnNumber(),
            null
        ));
        throw e;  // Arrêt de la validation
    }
    
    public List<ValidationError> getErrors() { return errors; }
    public List<ValidationError> getWarnings() { return warnings; }
}