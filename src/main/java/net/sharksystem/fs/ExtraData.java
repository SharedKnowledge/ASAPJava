package net.sharksystem.fs;

import net.sharksystem.SharkException;

import java.io.IOException;

public interface ExtraData {
    // remember a parameter with a key
    void putExtra(CharSequence key, byte[] value) throws IOException, SharkException;
    // get a parameter by a key
    byte[] getExtra(CharSequence key) throws IOException, SharkException;
    // remove all data
    void removeAll() throws IOException, SharkException;
}
