package net.sharksystem;

public class YourComponentFactory implements SharkComponentFactory {
    @Override
    public SharkComponent getComponent() {
        return new YourComponentImpl();
    }
}
