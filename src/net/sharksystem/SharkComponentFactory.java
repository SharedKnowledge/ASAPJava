package net.sharksystem;

/**
 * Factory that produces an instance of your ASAP app which can be used as Shark application component
 */
public interface SharkComponentFactory {
    /**
     * @return Instance of your application. It is up to you if your factory produces a singleton or not.
     */
    SharkComponent getComponent();
}
