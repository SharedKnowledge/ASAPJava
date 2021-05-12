package net.sharksystem;

public class SharkNotSupportedException extends RuntimeException {
    public SharkNotSupportedException() {
        super();
    }
    public SharkNotSupportedException(String message) {
        super(message);
    }
    public SharkNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
    public SharkNotSupportedException(Throwable cause) {
        super(cause);
    }
}
