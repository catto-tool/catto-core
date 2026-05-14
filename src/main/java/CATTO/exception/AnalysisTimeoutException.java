package CATTO.exception;

public class AnalysisTimeoutException extends Exception {
    public AnalysisTimeoutException(long timeoutSeconds) {
        super("CATTO analysis timed out after " + timeoutSeconds + " second(s)");
    }
}
