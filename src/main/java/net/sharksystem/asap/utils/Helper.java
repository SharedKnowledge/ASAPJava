package net.sharksystem.asap.utils;

import net.sharksystem.utils.Log;
import net.sharksystem.utils.Utils;
import net.sharksystem.asap.ASAPChunkStorage;
import net.sharksystem.asap.engine.ASAPEngine;
import net.sharksystem.asap.engine.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;

import java.io.*;
import java.util.*;

public class Helper {
    public static final String SERIALIZATION_DELIMITER = "|||";

    public static String collection2String(Collection<CharSequence> stringList) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        if(stringList != null && !stringList.isEmpty()) {
            for(CharSequence s : stringList) {
                if(s != null) {
                    if(!first) {
                        sb.append(SERIALIZATION_DELIMITER);
                    } else {
                        first = false;
                    }
                    sb.append(s);
                }
            }
        }

        return sb.toString();
    }

    public static byte[] long2byteArray(long value) {
        int numberBytes = Long.SIZE / 8; // A byte has 8 bits :) Fascinating, Captain.
        byte[] result = new byte[numberBytes];

        for(int index = numberBytes-1; index >= 0; index--) {
            long mask = 0xFF;
            // shift mask
            mask = mask << index;

            // mask all but indexed byte
            long resultLong = value & mask;

            // shift result to first byte
            resultLong = resultLong >> index;

            // take first byte only
            result[index] = (byte)resultLong;
        }

        return result;
    }

    public static byte[] characterSequence2bytes(CharSequence cs) {
        // TODO - that's ok?
        return cs.toString().getBytes();
    }

    public static CharSequence bytes2characterSequence(byte[] bytes) {
        // TODO - that's ok?
        return  new String(bytes);
    }

    public static List<CharSequence> string2CharSequenceList(String s) {
        StringTokenizer t = new StringTokenizer(s, SERIALIZATION_DELIMITER);
        ArrayList<CharSequence> list = new ArrayList<CharSequence>();

        while(t.hasMoreTokens()) {
            list.add(t.nextToken());
        }

        return list;
    }

    public static Set<CharSequence> string2CharSequenceSet(String s) {
        List<CharSequence> charSequences = Helper.string2CharSequenceList(s);
        Set charSet = new HashSet();
        for(CharSequence c : charSequences) {
            charSet.add(c);
        }

        return charSet;
    }

    public static ArrayList set2arraylist(Set aSet) {
        ArrayList aList = new ArrayList();
        if(aSet == null) return aList;

        for(Object o : aSet) {
            aList.add(o);
        }

        return aList;
    }

    public static Set list2set(List aList) {
        Set aSet = new HashSet();
        if(aList == null) return aSet;

        for(Object o : aList) {
            aSet.add(o);
        }

        return aSet;
    }

    public static byte[] str2bytes(String msg) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);
        daos.writeUTF(msg);

        return baos.toByteArray();
    }

    public static String bytes2str(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream daos = new DataInputStream(bais);
        return daos.readUTF();
    }

    public static ASAPMessages getMessagesByChunkReceivedInfos(String format, String sender, String uri,
                                                               String folderName, int era) {
        try {
            String rootIncomingStorage = folderName + "/" + Utils.url2FileName(format);
            Log.writeLog(Helper.class, "try getting storage in folder " + rootIncomingStorage);
            ASAPEngine existingASAPEngineFS =
                    ASAPEngineFS.getExistingASAPEngineFS(rootIncomingStorage);
            Log.writeLog(Helper.class, "got existing asap engine");

            ASAPChunkStorage chunkStorage = existingASAPEngineFS.getReceivedChunksStorage(sender);
            Log.writeLog(Helper.class, "got incoming channel of " + sender);

            ASAPMessages asapMessages = chunkStorage.getASAPMessages(uri, era, era);
            Log.writeLog(Helper.class, "got messages uri: " + uri + " / era: " + era);

            return asapMessages;
        } catch (IOException | ASAPException e) {
            Log.writeLog(Helper.class, "could not access message after be informed about new chunk arrival"
                            + e.getLocalizedMessage());
        }

        return null;
    }

    public static final boolean sameByteArray(byte[] a, byte[] b) {
        if(a == null && b == null) return true;
        if(a.length != b.length) return false;

        // not null, same length
        for(int i = 0; i < a.length; i++) {
            if(a[i] != b[i]) return false;
        }

        return true;
    }
}
