package net.sharksystem.asap.listenermanager;

public interface GenericNotifier<L> {
    /**
     * run specific notification
     */
    void doNotify(L listener);
}
