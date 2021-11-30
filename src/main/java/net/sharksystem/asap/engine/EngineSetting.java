package net.sharksystem.asap.engine;

public class EngineSetting {
    public final CharSequence folder;
    public ASAPChunkReceivedListener listener;
    public ASAPEngine engine;

    EngineSetting(CharSequence folder, ASAPChunkReceivedListener listener) {
        this.folder = folder;
        this.listener = listener;
    }

    void setASAPEngine(ASAPEngine engine) {
        this.engine = engine;
    }
}
