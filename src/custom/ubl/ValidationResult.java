package custom.ubl;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private List<ValidationError> errors = new ArrayList<>();
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public void addError(ValidationError error) { errors.add(error); }
    public void addErrors(List<ValidationError> errors) { this.errors.addAll(errors); }
    
    public void merge(ValidationResult other) {
        this.errors.addAll(other.errors);
    }
    
    public String getErrorsSummary() {
        return errors.stream()
            .map(e -> e.getMessage())
            .collect(java.util.stream.Collectors.joining("; "));
    }

    
    public List<ValidationError> getErrors() { return errors; }
}
