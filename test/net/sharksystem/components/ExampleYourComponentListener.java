package net.sharksystem.components;

public class ExampleYourComponentListener implements YourComponentListener {
    public int counterA = 0;
    public int counterB = 0;

    @Override
    public void somethingHappenedFormatA(CharSequence message) {
        System.out.println("A happened: " + message);
        this.counterA++;
    }

    @Override
    public void somethingHappenedFormatB(CharSequence message) {
        System.out.println("B happened: " + message);
        this.counterB++;
    }
}
