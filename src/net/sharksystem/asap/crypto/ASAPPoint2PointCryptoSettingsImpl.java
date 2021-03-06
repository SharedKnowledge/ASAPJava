package net.sharksystem.asap.crypto;

public class ASAPPoint2PointCryptoSettingsImpl implements ASAPPoint2PointCryptoSettings {
    private final boolean encrypt;
    private final boolean sign;

    public ASAPPoint2PointCryptoSettingsImpl(boolean encrypt, boolean sign) {
        this.encrypt = encrypt;
        this.sign = sign;
    }
    @Override
    public boolean mustEncrypt() {
        return this.encrypt;
    }

    @Override
    public boolean mustSign() {
        return this.sign;
    }
}
