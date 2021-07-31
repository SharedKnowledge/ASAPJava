package net.sharksystem;

public class SharkException extends Exception {
        public SharkException() { super(); }
        public SharkException(String message) {
            super(message);
        }
        public SharkException(String message, Throwable cause) {
            super(message, cause);
        }
        public SharkException(Throwable cause) {
            super(cause);
        }
}
