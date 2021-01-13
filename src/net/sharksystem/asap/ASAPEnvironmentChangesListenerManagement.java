package net.sharksystem.asap;

public interface ASAPEnvironmentChangesListenerManagement {
    /**
     * Add listener for changes in ASAP environment
     * @param changesListener listener which is to be added
     */
    void addASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener);

    /**
     * Remove changes listener
     * @param changesListener listener which is to be removed
     */
    void removeASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener);
}
