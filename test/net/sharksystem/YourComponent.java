package net.sharksystem;

@ASAPFormats(formats = {YourComponent.FORMAT_A, YourComponent.FORMAT_B})
public interface YourComponent extends SharkComponent {
    String FORMAT_A = "myApp://formatA";
    String FORMAT_B = "myApp://formatB";
}
