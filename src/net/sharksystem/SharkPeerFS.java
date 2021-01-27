package net.sharksystem;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SharkPeerFS implements SharkPeer {
    protected final CharSequence owner;
    protected final CharSequence rootFolder;
    private HashMap<CharSequence, SharkComponentFactory> factories = new HashMap<>();
    protected HashMap<CharSequence, SharkComponent> components = new HashMap<>();
    private SharkPeerStatus status = SharkPeerStatus.NOT_INITIALIZED;
    private ASAPPeerFS asapPeer;

    public SharkPeerFS(CharSequence owner, CharSequence rootFolder) {
        this.owner = owner;
        this.rootFolder = rootFolder;
    }

    private static final String MISSING_FORMAT_EXCEPTION_MESSAGE =
            "annotation missing or parameters parameters: " +
            "@" + ASAPFormats.class.getSimpleName() + "(formats = {\"formatA\"[, \"formatB\"]})";

    private Set<String> getFormats(Class<? extends SharkComponent> facade) throws SharkException {
        try {
            ASAPFormats annotation = facade.getAnnotation(ASAPFormats.class);
            String[] formats = annotation.formats();
            if(formats == null || formats.length < 1) {
                throw new SharkException(MISSING_FORMAT_EXCEPTION_MESSAGE);
            }
            Set<String> formatSet = new HashSet<>();
            for(int i = 0; i < formats.length; i++) {
                formatSet.add(formats[i]);
            }

            return formatSet;
        }
        catch(RuntimeException re) {
            throw new SharkException(MISSING_FORMAT_EXCEPTION_MESSAGE);
        }
    }

    @Override
    public void addComponent(SharkComponentFactory componentFactory,
                             Class<? extends SharkComponent> facade)
            throws SharkException {

        if(this.status != SharkPeerStatus.NOT_INITIALIZED) {
            throw new SharkException("Components cannot be added to a running Shark Peer");
        }

        Set<String> componentFormats = this.getFormats(facade);

        if(componentFormats == null || componentFormats.size() < 1) {
            throw new SharkException("Components must support at least one format. Got null or empty list");
        }

        for(CharSequence format : componentFormats) {
            if(this.components.get(format) != null) {
                throw new SharkException("There is already a component with using format: " + format);
            }

            for(int i = 0; i < SharkComponent.reservedFormats.length; i++) {
                if(format.toString().equalsIgnoreCase(SharkComponent.reservedFormats[i].toString())) {
                    throw new SharkException("You must not use a reserved format: " + format);
                }
            }
        }

        Log.writeLog(this, "create component");
        SharkComponent component = componentFactory.getComponent();

        for(CharSequence format : componentFormats) {
            Log.writeLog(this, "added component that supports format " + format);
            this.components.put(format, component);
        }
    }

    @Override
    public void removeComponent(Class<? extends SharkComponent> facade)
            throws SharkException {

        if(this.status != SharkPeerStatus.NOT_INITIALIZED) {
            throw new SharkException("Components cannot be removed from a running Shark Peer");
        }

        Set<String> componentFormats = this.getFormats(facade);

        for(CharSequence format : componentFormats) {
            this.components.remove(format);
            Log.writeLog(this, "removed component that supported format " + format);
        }
    }

    @Override
    public SharkComponent getComponent(Class<? extends SharkComponent> facade) throws SharkException {
        Set<String> formats = this.getFormats(facade);
        StringBuilder sb = new StringBuilder();
        for(CharSequence format: formats) {
            SharkComponent component = this.components.get(format);
            if(component != null) return component;
            sb.append(format);
            sb.append(" | ");
        }

        throw new SharkException("no component found with format(s) " + sb.toString());
    }

    protected ASAPPeerFS createASAPPeer() throws IOException, ASAPException {
        return new ASAPPeerFS(this.owner, this.rootFolder, this.components.keySet());
    }

    @Override
    public void start() throws SharkException {
        if(this.status != SharkPeerStatus.NOT_INITIALIZED) {
            throw new SharkException("Shark Peer is already running");
        }

        try {
            this.asapPeer = this.createASAPPeer();
        } catch (IOException | ASAPException e) {
            Log.writeLogErr(this, "could not start ASAP peer - fatal, give up");
            throw new SharkException(e);
        }

        boolean fullSuccess = true; // optimistic
        for(SharkComponent component : this.components.values()) {
            try {
                component.onStart(this.asapPeer);
            } catch (SharkException e) {
                fullSuccess = false;
                Log.writeLogErr(this, "could not start component: " + e.getLocalizedMessage());
            }
        }

        this.status = SharkPeerStatus.RUNNING;
        if(fullSuccess) {
            Log.writeLog(this, "Shark system started");
        } else {
            Log.writeLog(this, "Shark system started with errors.");
        }
    }

    @Override
    public void stop() throws SharkException {
        if(this.status != SharkPeerStatus.RUNNING) {
            throw new SharkException("Shark Peer is not running");
        }
        Log.writeLog(this, "Shark system stopped");
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
