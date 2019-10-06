package net.sharksystem.cmdline;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.MultiASAPEngineFS;

import java.io.IOException;

class TCPChannelCreatedHandler implements TCPChannelCreatedListener {
    private final MultiASAPEngineFS multiASAPEngineFS;

    public TCPChannelCreatedHandler(MultiASAPEngineFS multiASAPEngineFS) {
        this.multiASAPEngineFS = multiASAPEngineFS;
    }

    @Override
    public void channelCreated(TCPChannel channel) {
        System.out.println("Channel created");

        try {
            this.multiASAPEngineFS.handleConnection(
                    channel.getInputStream(),
                    channel.getOutputStream());
        } catch (IOException | ASAPException e) {
            System.err.println("call of engine.handleConnection failed: " + e.getLocalizedMessage());
        }
    }
}
