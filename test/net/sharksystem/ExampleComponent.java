package net.sharksystem;

@ASAPFormats(formats = {ExampleComponent.FORMAT_A, ExampleComponent.FORMAT_B})
public interface ExampleComponent extends SharkComponent {
    String FORMAT_A = "myApp://formatA";
    String FORMAT_B = "myApp://formatB";
}
