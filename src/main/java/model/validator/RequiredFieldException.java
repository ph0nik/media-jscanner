package model.validator;

public class RequiredFieldException extends Exception {
    private final String fieldName;
    public RequiredFieldException(String s) {
        fieldName = s;
    }

    @Override
    public String getMessage() {
        return this.getClass().getName() + "\n" + fieldName + " cannot be null";
    }
}
