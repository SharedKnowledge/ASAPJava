package net.sharksystem.asap.engine;

public class EngineSetting {
    public final CharSequence folder;
    public ASAPChunkAssimilatedListener listener;
    public ASAPEngine engine;

    EngineSetting(CharSequence folder, ASAPChunkAssimilatedListener listener) {
        this.folder = folder;
        this.listener = listener;
    }

    void setASAPEngine(ASAPEngine engine) {
        this.engine = engine;
    }
}
