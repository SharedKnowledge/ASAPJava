package net.sharksystem.utils;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPSerialization {
    public static void writeByteArray(byte[] bytes2Write, OutputStream os) throws IOException {
        writeNonNegativeIntegerParameter(bytes2Write.length, os);
        os.write(bytes2Write);
    }

    public static byte[] readByteArray(InputStream is) throws IOException, ASAPException {
        // read len
        int len = readIntegerParameter(is);
        byte[] messageBytes = new byte[len];

        // read encrypted bytes from stream
        is.read(messageBytes);

        return messageBytes;
    }

    public static void writeCharSequenceParameter(CharSequence parameter, OutputStream os) throws IOException {
        if(parameter == null || parameter.length() < 1) return;
        byte[] bytes = parameter.toString().getBytes();
        writeNonNegativeIntegerParameter(bytes.length, os);
        os.write(bytes);
    }

    public static void writeByteParameter(byte parameter, OutputStream os) throws IOException {
        os.write(new byte[] { parameter} );
    }

    public static void writeShortParameter(short parameter, OutputStream os) throws IOException {
        // short = 16 bit = 2 bytes
        int leftInt = parameter >> 8;
        writeByteParameter( (byte)leftInt, os);
        // cut left part
        writeByteParameter( (byte)parameter, os);
    }

    public static void writeNonNegativeIntegerParameter(int parameter, OutputStream os) throws IOException {
        if(parameter < 0) return; // non negative!

        // Integer == 32 bit == 4 Byte
        int left = parameter >> 16;
        writeShortParameter((short) left, os);
        writeShortParameter((short) parameter, os);
    }

    public static void writeNonNegativeLongParameter(long longValue, OutputStream os) throws IOException {
        if(longValue < 0) return;

        // Long = 64 bit = 2 Integer
        long left = longValue >> 32;
        writeNonNegativeIntegerParameter((int) left, os);
        writeNonNegativeIntegerParameter((int) longValue, os);
    }

    public static byte readByteParameter(InputStream is) throws IOException, ASAPException {
        return readByte(is);
    }

    public static byte readByte(InputStream is) throws IOException, ASAPException {
        int value = is.read();
        if(value < 0) {
            throw new ASAPException("read -1: no more data in stream");
        }
        return (byte) value;
    }

    public static short readShortParameter(InputStream is) throws IOException, ASAPException {
        int value = readByteParameter(is);
        value = value << 8;
        int right = readByteParameter(is);
        value += right;
        return (short) value;
    }

    public static int readIntegerParameter(InputStream is) throws IOException, ASAPException {
        int value = readShortParameter(is);
        value = value << 16;
        int right = readShortParameter(is);
        value += right;
        return value;
    }

    public static long readLongParameter(InputStream is) throws IOException, ASAPException {
        long value = readIntegerParameter(is);
        value = value << 32;
        long right = readIntegerParameter(is);
        value += right;
        return value;
    }

    public static String readCharSequenceParameter(InputStream is) throws IOException, ASAPException {
        int length = readIntegerParameter(is);
        byte[] parameterBytes = new byte[length];

        is.read(parameterBytes);

        return new String(parameterBytes);
    }
}
