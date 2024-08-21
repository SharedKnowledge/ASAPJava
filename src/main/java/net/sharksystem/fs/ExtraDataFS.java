package net.sharksystem.fs;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExtraDataFS implements ExtraData {
    public static final String DEFAULT_FILE_NAME = ".extraData";
    private final CharSequence fileName;
    private final CharSequence rootFolderName;

    private Map<CharSequence, byte[]> extraData = new HashMap<>();

    public ExtraDataFS(CharSequence rootFolderName) throws SharkException, IOException {
        this(rootFolderName, DEFAULT_FILE_NAME);
    }

    public ExtraDataFS(CharSequence rootFolder, CharSequence fileName) throws IOException, SharkException {
        this.rootFolderName = rootFolder;
        // hide file in any case
        if(fileName.charAt(0) != '.') this.fileName = "." + fileName;
        else this.fileName = fileName;
        File rootFolderFile = new File(rootFolder.toString());
        if(!rootFolderFile.exists()) {
            // create
            rootFolderFile.mkdirs();
        }
        this.restoreExtraData();
    }

    private File getExtraFile() {
        String extraFileName = this.rootFolderName + "/" + fileName;
        return new File(extraFileName);
    }

    /*
    Here is the catch... There can be - and in Android will - two instances share data over file system. The use
    same clock but run in different threads, most likely different processes. We have to synchronize both sides.

    There will be two processes A and B. Let's assume we are in process A.
    a) We never written something,

     */

    private long timeStampSyncExtraData = -1; // never saved anything

    private void extraDataSync() throws IOException, SharkException {
        InputStream is = null;
        try {
            is = new FileInputStream(this.getExtraFile());
        }
        catch(IOException ioEx) {
            // no such file - save it instead - there is no conflict
            this.saveExtraData();
            return;
        }

        // read time stamp
        long timeStampSaved = ASAPSerialization.readLongParameter(is);

        if(this.timeStampSyncExtraData < timeStampSaved) {
            // there is an external copy that was created after our last sync
        }

        // discard local changes and read from file
        this.restoreExtraData();
    }

    public void saveExtraData() throws IOException {
        OutputStream os = new FileOutputStream(this.getExtraFile());

        // write time stamp
        this.timeStampSyncExtraData = System.currentTimeMillis();
        ASAPSerialization.writeLongParameter(this.timeStampSyncExtraData, os);
        ASAPSerialization.writeNonNegativeIntegerParameter(this.extraData.size(), os);

        for(CharSequence key : this.extraData.keySet()) {
            // write key
            ASAPSerialization.writeCharSequenceParameter(key, os);
            // value
            byte[] v = this.extraData.get(key);
            ASAPSerialization.writeByteArray(v, os);
        }

        os.close();
    }

    /**
     * Always restore. If there is no such file - write it.
     * @throws IOException
     * @throws ASAPException
     */
    public void restoreExtraData() throws IOException, SharkException {
        InputStream is = null;
        try {
            is = new FileInputStream(this.getExtraFile());
        }
        catch(IOException ioEx) {
            // no such file - nothing to do here
            return;
        }

        long timeStampSaved = ASAPSerialization.readLongParameter(is);
        if(timeStampSaved == this.timeStampSyncExtraData) {
            is.close();
            return;
        }

        // something changed - get a fresh copy
        this.timeStampSyncExtraData = timeStampSaved;

        this.extraData = new HashMap<>();
        int counter = ASAPSerialization.readIntegerParameter(is);

        while(counter-- > 0) {
            // read key
            CharSequence key = ASAPSerialization.readCharSequenceParameter(is);
            // value
            byte[] value = ASAPSerialization.readByteArray(is);

            // save in memory
            this.extraData.put(key, value);
        }
        is.close();
    }

    /**
     * Make a value persistent with key
     * @param key
     * @param value
     */
    public void putExtra(CharSequence key, byte[] value) throws IOException, SharkException {
        this.extraDataSync();
        this.extraData.put(key, value);
        this.saveExtraData();
    }

    @Override
    public void putExtra(CharSequence key, Integer value) throws IOException, SharkException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeIntegerParameter(value, baos);
        this.putExtra(key, baos.toByteArray());
    }

    @Override
    public void putExtra(CharSequence key, CharSequence value) throws IOException, SharkException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeCharSequenceParameter(value, baos);
        this.putExtra(key, baos.toByteArray());
    }

    public void putExtra(CharSequence key, Set<CharSequence> value) throws IOException, SharkException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeCharSequenceSetParameter(value, baos);
        this.putExtra(key, baos.toByteArray());
    }

    /**
     * Return a value - can be null if null was set as value.
     * @param key
     * @throws ASAPException key never used in putExtra
     */
    public byte[] getExtra(CharSequence key) throws IOException, SharkException {
        this.restoreExtraData();
        byte[] value = this.extraData.get(key);
        if(value == null) throw new SharkException("no value for key" + key);
        return value;
    }

    @Override
    public int getExtraInteger(CharSequence key) throws IOException, SharkException {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.getExtra(key));
        return ASAPSerialization.readIntegerParameter(bais);
    }

    @Override
    public String getExtraString(CharSequence key) throws IOException, SharkException {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.getExtra(key));
        return ASAPSerialization.readCharSequenceParameter(bais);
    }

    public Set<CharSequence> getExtraCharSequenceSetParameter(CharSequence key) throws IOException, SharkException {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.getExtra(key));
        return ASAPSerialization.readCharSequenceSetParameter(bais);
    }

    @Override
    public void removeAll() {
        this.getExtraFile().delete();
    }
}
