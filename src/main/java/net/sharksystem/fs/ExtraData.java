package net.sharksystem.fs;

import net.sharksystem.SharkException;

import java.io.IOException;
import java.util.Set;

public interface ExtraData {
    // remember a parameter with a key
    void putExtra(CharSequence key, byte[] value) throws IOException, SharkException;

    void putExtra(CharSequence key, Integer value) throws IOException, SharkException;

    void putExtra(CharSequence key, String value) throws IOException, SharkException;

    void putExtra(CharSequence key, Set<CharSequence> value) throws IOException, SharkException;

    // get a parameter by a key
    byte[] getExtra(CharSequence key) throws IOException, SharkException;

    int getExtraInteger(CharSequence key) throws IOException, SharkException;

    Set<CharSequence> getExtraCharSequenceSetParameter(CharSequence key) throws IOException, SharkException;

    String getExtraString(CharSequence key) throws IOException, SharkException;
    // remove all data
    void removeAll() throws IOException, SharkException;
}
