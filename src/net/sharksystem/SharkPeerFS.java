package net.sharksystem;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPPeerFS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class SharkPeerFS implements SharkPeer {
    private final CharSequence owner;
    private final CharSequence rootFolder;
    private HashMap<CharSequence, SharkComponentFactory> factories = new HashMap<>();
    private HashMap<CharSequence, SharkComponent> components = new HashMap<>();
    private SharkPeerStatus status = SharkPeerStatus.NOT_INITIALIZED;
    private ASAPPeerFS asapPeer;

    public SharkPeerFS(CharSequence owner, CharSequence rootFolder) {
        this.owner = owner;
        this.rootFolder = rootFolder;
    }

    @Override
    public void addComponent(SharkComponentFactory componentFactory) throws SharkException {
        if(this.status != SharkPeerStatus.NOT_INITIALIZED) {
            throw new SharkException("Components cannot be added to a running Shark Peer");
        }

        Set<CharSequence> componentFormats = componentFactory.getSupportedFormats();
        for(CharSequence format : componentFormats) {
            if(this.components.get(format) != null) {
                throw new SharkException("There is already a component with using format: " + format);
            }
        }

        Log.writeLog(this, "create component");
        SharkComponent component = componentFactory.getInstance();

        for(CharSequence format : componentFormats) {
            this.components.put(format, component);
        }
    }

    @Override
    public void removeComponent(SharkComponent component) throws SharkException {
        if(this.status != SharkPeerStatus.NOT_INITIALIZED) {
            throw new SharkException("Components cannot be removed from a running Shark Peer");
        }

        Set<CharSequence> componentFormats = component.getSupportedFormats();
        Log.writeLog(this, "remove component");
        for(CharSequence format : componentFormats) {
            this.components.remove(format);
        }
    }

    @Override
    public SharkComponent getComponent(CharSequence format) throws SharkException {
        SharkComponent sharkComponent = this.components.get(format);
        if(sharkComponent == null)
            throw new SharkException("no component found with format " + format);

        return sharkComponent;
    }

    @Override
    public void start() throws SharkException {
        if(this.status != SharkPeerStatus.NOT_INITIALIZED) {
            throw new SharkException("Shark Peer is already running");
        }

        try {
            this.asapPeer = new ASAPPeerFS(this.owner, this.rootFolder, this.components.keySet());
            // inform all peers
            for(SharkComponent component : this.components.values()) {
                component.onStart(this.asapPeer);
            }

            this.status = SharkPeerStatus.RUNNING;
        } catch (IOException | ASAPException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws SharkException {
        if(this.status != SharkPeerStatus.RUNNING) {
            throw new SharkException("Shark Peer is not running");
        }

    }

    @Override
    public SharkPeerStatus getStatus() {
        return this.status;
    }

    @Override
    public ASAPPeer getASAPPeer() throws SharkException {
        if(this.status != SharkPeerStatus.RUNNING) {
            throw new SharkException("Shark Peer is not running");
        }

        if(this.asapPeer == null) {
            throw new SharkException("That's a bug: ASAP peer not created");
        }

        return this.asapPeer;
    }
}
