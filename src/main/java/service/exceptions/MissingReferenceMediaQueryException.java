package service.exceptions;

public class MissingReferenceMediaQueryException extends Throwable {
    public MissingReferenceMediaQueryException() {
        super("Missing reference media query");
    }
}
