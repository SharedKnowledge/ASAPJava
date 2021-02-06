package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPException;

public class SharkCryptoException extends ASAPException {
        public SharkCryptoException() { super(); }

        public SharkCryptoException(String message) { super(message); }

        public SharkCryptoException(String message, Throwable cause) { super(message, cause); }

        public SharkCryptoException(Throwable cause) { super(cause); }
}
