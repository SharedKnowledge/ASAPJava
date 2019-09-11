package net.sharksystem.asap;

class EngineSetting {
    final CharSequence folder;
    ASAPChunkReceivedListener listener;
    ASAPEngine engine;

    EngineSetting(CharSequence folder, ASAPChunkReceivedListener listener) {
        this.folder = folder;
        this.listener = listener;
    }

    void setASAPEngine(ASAPEngine engine) {
        this.engine = engine;
    }
}
