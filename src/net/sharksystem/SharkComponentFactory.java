package net.sharksystem;

import java.util.Set;

/**
 * Factory that produces an instance of your ASAP app which can be used as Shark application component
 */
public interface SharkComponentFactory {
    /**
     * @return Instance of your application. It is up to you if your factory produces a singleton or not.
     */
    SharkComponent getInstance();

    /**
     * @return all format supported by your application. Format of all components are merged and used
     * to set up a single ASAP peer for the application.
     */
    Set<CharSequence> getSupportedFormats();
}
