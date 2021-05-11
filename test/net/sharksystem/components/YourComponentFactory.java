package net.sharksystem.components;

import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class YourComponentFactory implements SharkComponentFactory {
    @Override
    public SharkComponent getComponent() {
        return new YourComponentImpl();
    }
}
