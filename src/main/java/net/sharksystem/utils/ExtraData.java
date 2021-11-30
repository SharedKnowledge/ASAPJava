package net.sharksystem.utils;

import net.sharksystem.SharkException;

import java.io.IOException;

public interface ExtraData {
    void putExtra(CharSequence key, byte[] value) throws IOException, SharkException;
    byte[] getExtra(CharSequence key) throws IOException, SharkException;
}
