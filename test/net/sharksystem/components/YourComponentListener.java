package net.sharksystem.components;

/**
 * define messages your component is going to publish to applications using your component
 */
public interface YourComponentListener {
    void somethingHappenedFormatA(CharSequence message);
    void somethingHappenedFormatB(CharSequence message);
}
