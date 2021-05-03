package net.sharksystem.asap.utils;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class ASAPSerialization {
    public static final long BLANK_LEFT_LONG = 0x00000000FFFFFFFFL;
    public static final long BLANK_RIGHT_LONG = 0xFFFFFFFF00000000L;
    public static final int BLANK_LEFT_INTEGER = 0x0000FFFF;
    public static final int BLANK_RIGHT_INTEGER = 0xFFFF0000;
    public static final short BLANK_LEFT_SHORT = 0x00FF;
    public static final short BLANK_RIGHT_SHORT = (short) 0xFF00;

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

    public static void writeShortParameter(short shortValue, OutputStream os) throws IOException {
        // short = 16 bit = 2 bytes
        short left = (short) (shortValue & BLANK_RIGHT_SHORT);
        left = (short) (left >> 8);
        short right = (short) (shortValue & BLANK_LEFT_SHORT);
        writeByteParameter( (byte)left, os);
        // cut left part
        writeByteParameter( (byte)right, os);
    }

    public static void writeNonNegativeIntegerParameter(int parameter, OutputStream os) throws IOException {
        if(parameter < 0) return; // non negative!

        // Integer == 32 bit == 4 Byte
        int left = parameter >> 16;
        writeShortParameter((short) left, os);
        writeShortParameter((short) parameter, os);
    }

    public static void writeIntegerParameter(int intValue, OutputStream os) throws IOException {
        // Integer == 32 bit == 4 Byte
        int left = intValue & BLANK_RIGHT_INTEGER;
        left = left >> 16;
        int right = intValue & BLANK_LEFT_INTEGER;
        writeShortParameter((short) left, os);
        writeShortParameter((short) right, os);
    }

    public static void writeNonNegativeLongParameter(long longValue, OutputStream os) throws IOException {
        if(longValue > -1) writeLongParameter(longValue, os);
        else throw new IOException("negative value");
    }

    public static void writeLongParameter(long longValue, OutputStream os) throws IOException {
        // Long = 64 bit = 2 Integer
        long left = longValue & BLANK_RIGHT_LONG;
        left = left >> 32;
        long right = longValue & BLANK_LEFT_LONG;
        writeIntegerParameter((int)left, os);
        writeIntegerParameter((int)right, os);
    }

    public static void printBits(long l, int bits) {
        long mask = 1;
        mask = mask << bits-1;
        short byteBitCounter = 4;
        while(mask != 0) {
            if((l & mask) != 0) System.out.print("1");
            else System.out.print("0");
            if(--byteBitCounter == 0) {
                byteBitCounter = 4;
                System.out.print(" ");
            }
            mask = mask >> 1;
        }
        System.out.print(" ");
    }

    public static void printByteArray(byte[] byteArray) {
        for(int index = byteArray.length - 1; index >= 0; index--) {
            printByte(byteArray[index]);
        }
    }

    public static void printByte(short s) { printBits(s, 8); }
    public static void printBits(short s) { printBits(s, 16); }
    public static void printBits(int i) { printBits(i, 32); }
    public static void printBits(long l) {
        long left = l & BLANK_RIGHT_LONG;
        left = left >> 32;
        printBits((int) left);
        long right = l & BLANK_LEFT_LONG;
        printBits((int) right);
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
        // by sure
        value = value & BLANK_RIGHT_SHORT;
        int right = readByteParameter(is);
        // by sure
        right = right & BLANK_LEFT_SHORT;
        value += right;

        return (short) value;
    }

    public static int readIntegerParameter(InputStream is) throws IOException, ASAPException {
        int value = readShortParameter(is);
        value = value << 16;
        value = value & BLANK_RIGHT_INTEGER;

        int right = readShortParameter(is);
        right = right & BLANK_LEFT_INTEGER;

        value += right;

        return value;
    }

    public static long readLongParameter(InputStream is) throws IOException, ASAPException {
        long value = readIntegerParameter(is);
        value = value << 32;
        value = value & BLANK_RIGHT_LONG;

        long right = readIntegerParameter(is);
        right = right & BLANK_LEFT_LONG;

        value += right;

        /*
        System.out.println("readLongParameter");
        printBits(value);
        System.out.print("\n");
         */

        return value;
    }

    public static String readCharSequenceParameter(InputStream is) throws IOException, ASAPException {
        int length = readIntegerParameter(is);
        byte[] parameterBytes = new byte[length];

        is.read(parameterBytes);

        return new String(parameterBytes);
    }

    public static void writeCharSequenceSetParameter(Set<CharSequence> charSet, OutputStream os) throws IOException {
        if(charSet == null || charSet.size() == 0) {
            ASAPSerialization.writeNonNegativeIntegerParameter(0, os);
            return;
        }

        // more recipients

        // write len
        ASAPSerialization.writeNonNegativeIntegerParameter(charSet.size(), os);

        // write entries
        for(CharSequence entry : charSet) {
            ASAPSerialization.writeCharSequenceParameter(entry, os);
        }
    }

    public static Set<CharSequence> readCharSequenceSetParameter(InputStream is) throws IOException, ASAPException {
        Set<CharSequence> charSet = new HashSet<>();
        int len = ASAPSerialization.readIntegerParameter(is);

        while(len-- > 0) {
            charSet.add(ASAPSerialization.readCharSequenceParameter(is));
        }

        return charSet;
    }
}
