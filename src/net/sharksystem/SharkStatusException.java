package net.sharksystem;

public class SharkStatusException extends RuntimeException {
    public SharkStatusException() {
        super();
    }
    public SharkStatusException(String message) {
        super(message);
    }
    public SharkStatusException(String message, Throwable cause) {
        super(message, cause);
    }
    public SharkStatusException(Throwable cause) {
        super(cause);
    }
}
