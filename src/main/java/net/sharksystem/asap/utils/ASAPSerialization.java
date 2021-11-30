package net.sharksystem.asap.utils;

import net.sharksystem.asap.*;
import net.sharksystem.utils.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ASAPSerialization {
    public static final long BLANK_LEFT_LONG = 0x00000000FFFFFFFFL;
    public static final long BLANK_RIGHT_LONG = 0xFFFFFFFF00000000L;
    public static final int BLANK_LEFT_INTEGER = 0x0000FFFF;
    public static final int BLANK_RIGHT_INTEGER = 0xFFFF0000;
    public static final short BLANK_LEFT_SHORT = 0x00FF;
    public static final short BLANK_RIGHT_SHORT = (short) 0xFF00;

    public static void writeByteArray(byte[] bytes2Write, OutputStream os) throws IOException {
        if(bytes2Write == null) {
            writeNonNegativeIntegerParameter(0, os);
        } else {
            writeNonNegativeIntegerParameter(bytes2Write.length, os);
            os.write(bytes2Write);
        }
    }

    public static byte[] readByteArray(InputStream is) throws IOException, ASAPException {
        // read len
        int len = readIntegerParameter(is);
        if(len == 0) return new byte[0];

        byte[] messageBytes = new byte[len];
        // read encrypted bytes from stream
        is.read(messageBytes);

        return messageBytes;
    }

    public static void writeByteArray(byte[][] bytes2Dim, OutputStream os) throws IOException {
        if(bytes2Dim == null) {
            writeNonNegativeIntegerParameter(0, os);
        } else {
            writeNonNegativeIntegerParameter(bytes2Dim.length, os);
            for(int i = 0; i < bytes2Dim.length; i++) {
                ASAPSerialization.writeByteArray(bytes2Dim[i], os);
            }
        }
    }

    public static byte[][] readByte2DimArray(InputStream is) throws IOException, ASAPException {
        // read len
        int len = readIntegerParameter(is);
        if(len == 0) return new byte[0][];

        byte[][] messageBytes = new byte[len][];
        for(int i = 0; i < len; i++) {
            messageBytes[i] = ASAPSerialization.readByteArray(is);
        }

        return messageBytes;
    }

    public static void writeCharSequenceParameter(CharSequence parameter, OutputStream os) throws IOException {
        if(parameter == null || parameter.length() < 1) return;
        byte[] bytes = parameter.toString().getBytes();
        writeNonNegativeIntegerParameter(bytes.length, os);
        os.write(bytes);

        /*
        String lenString = printBitsToString(bytes.length);
        String readBytesString = printByteArrayToString(bytes);
        String log = "writeCharSequenceParameter\nlen: " + lenString + "\nbytes: " + readBytesString;
        System.out.println(log);
         */
    }

    public static void writeByteParameter(byte parameter, OutputStream os) throws IOException {
        os.write(new byte[] { parameter} );
    }

    /**
     * Return a byte from an int - position starts with 0 at least significant bit
     * @param source
     * @param position
     * @return
     */
    public static byte getByteFromInt(int source, int position) {
        int bitCountShift = position * 8; // byte has got 8 bit
        int tmpInt = source >> bitCountShift;
        return (byte) (tmpInt);
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

    public static String printBitsToString(long l, int bits) {
        StringBuilder sb = new StringBuilder();

        long mask = 1;
        mask = mask << bits-1;
        short byteBitCounter = 4;
        while(mask != 0) {
            if((l & mask) != 0) sb.append("1");
            else sb.append("0");
            if(--byteBitCounter == 0) {
                byteBitCounter = 4;
                sb.append(" ");
            }
            mask = mask >> 1;
        }
        sb.append(" ");

        return sb.toString();
    }

    public static String printByteArrayToString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for(int index = byteArray.length - 1; index >= 0; index--) {
            sb.append("[");
            sb.append(index);
            sb.append("]: ");
            sb.append(printByteToString(byteArray[index]));
        }
        return sb.toString();
    }

    public static String printByteToString(short s) { return printBitsToString(s, 8); }
    public static String printBitsToString(int i) { return printBitsToString(i, 16); }

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
        // be sure
        value = value & BLANK_RIGHT_SHORT;
        int right = readByteParameter(is);
        // be sure
        right = right & BLANK_LEFT_SHORT;
        value += right;

        return (short) value;
    }

    public static int readIntegerParameter(InputStream is) throws IOException, ASAPException {
        int value = 0;
        value = readShortParameter(is);
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
        int length = 0;
        byte[] parameterBytes = null;
        String lenString = null;
//        try {
            length = readIntegerParameter(is);
/*
            lenString = printBitsToString(length);
            System.out.println("readCharSequenceParameter length = " + lenString);

 */
            parameterBytes = new byte[length];
/*
        }
        catch(OutOfMemoryError e) {
            int i = 42;
            throw new ASAPException(e);
        }
 */

        is.read(parameterBytes);

        /*
        String readBytesString = printByteArrayToString(parameterBytes);
        String log = "readCharSequenceParameter\nlen: " + lenString + "\nbytes: " + readBytesString;
        System.out.println(log);
         */

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

    public static EncounterConnectionType readEncounterConnectionType(InputStream is) throws IOException, ASAPException {
        byte readByte = ASAPSerialization.readByte(is);
        switch(readByte) {
            case 1: return EncounterConnectionType.ASAP_HUB;
            case 2: return EncounterConnectionType.AD_HOC_LAYER_2_NETWORK;
            case 3: return EncounterConnectionType.ONION_NETWORK;
            case 4: return EncounterConnectionType.INTERNET;
            default:
                Log.writeLogErr(ASAPSerialization.class, "unknown encounter connection type: " + readByte);
        }
        // default
        return EncounterConnectionType.UNKNOWN;
    }

    public static void writeEncounterConnectionType(EncounterConnectionType connectionType, OutputStream os) throws IOException {
        switch(connectionType) {
            case ASAP_HUB: ASAPSerialization.writeByteParameter((byte) 1, os); break;
            case AD_HOC_LAYER_2_NETWORK: ASAPSerialization.writeByteParameter((byte) 2, os); break;
            case ONION_NETWORK: ASAPSerialization.writeByteParameter((byte) 3, os); break;
            case INTERNET: ASAPSerialization.writeByteParameter((byte) 4, os); break;
            case UNKNOWN:
            default: ASAPSerialization.writeByteParameter((byte) 0, os);
        }
    }

    public static void writeBooleanParameter(boolean value, OutputStream os) throws IOException {
        if(value) ASAPSerialization.writeByteParameter((byte) 1, os);
        else ASAPSerialization.writeByteParameter((byte) 0, os);
    }

    public static boolean readBooleanParameter(InputStream is) throws IOException, ASAPException {
        return ASAPSerialization.readByte(is) == 1;
    }

    public static void writeASAPHop(ASAPHop asapHop, OutputStream os) throws IOException {
        ASAPSerialization.writeCharSequenceParameter(asapHop.sender(), os);
        ASAPSerialization.writeBooleanParameter(asapHop.verified(), os);
        ASAPSerialization.writeBooleanParameter(asapHop.encrypted(), os);
        ASAPSerialization.writeEncounterConnectionType(asapHop.getConnectionType(), os);
    }

    public static byte[] asapHop2ByteArray(ASAPHop asapHop) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeASAPHop(asapHop, baos);
        return baos.toByteArray();
    }

    public static byte[] asapHopList2ByteArray(List<ASAPHop> asapHops) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeASAPHopList(asapHops, baos);
        return baos.toByteArray();
    }

    public static ASAPHop readASAPHop(InputStream is) throws IOException, ASAPException {
        CharSequence sender = ASAPSerialization.readCharSequenceParameter(is);
        boolean verified = ASAPSerialization.readBooleanParameter(is);
        boolean encrypted = ASAPSerialization.readBooleanParameter(is);
        EncounterConnectionType connectionType = ASAPSerialization.readEncounterConnectionType(is);

        return new ASAPHopImpl(sender, verified, encrypted, connectionType);
    }

    public static ASAPHop byteArray2ASAPHop(byte[] bytes) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return readASAPHop(bais);
    }

    public static List<ASAPHop> byteArray2ASAPHopList(byte[] bytes) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return readASAPHopList(bais);
    }

    public static void writeASAPHopList(List<ASAPHop> asapHopList, OutputStream os) throws IOException {
        if(asapHopList == null || asapHopList.isEmpty()) {
            // no hops
            ASAPSerialization.writeIntegerParameter(0, os);
            return;
        }

        // write number of hops
        ASAPSerialization.writeIntegerParameter(asapHopList.size(), os);
        for(ASAPHop asapHop : asapHopList) {
            ASAPSerialization.writeASAPHop(asapHop, os);
        }
    }

    public static List<ASAPHop> readASAPHopList(InputStream is) throws IOException, ASAPException {
        List<ASAPHop> asapHopList = new ArrayList<>();

        int number = ASAPSerialization.readIntegerParameter(is);
        while(number-- > 0) {
            asapHopList.add(ASAPSerialization.readASAPHop(is));
        }

        return asapHopList;

    }
}
