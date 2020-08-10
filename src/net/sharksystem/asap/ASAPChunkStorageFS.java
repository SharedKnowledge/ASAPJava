package net.sharksystem.asap;

import net.sharksystem.Utils;
import net.sharksystem.asap.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.sharksystem.asap.ASAPChunkFS.META_DATA_EXTENSION;

/**
 *
 * @author thsc
 */
class ASAPChunkStorageFS implements ASAPChunkStorage {
    private final String rootDirectory;
    private final String format;
    private int era = -1;

    ASAPChunkStorageFS(String rootDirectory, String format) {
        this.rootDirectory = rootDirectory;
        this.format = format;
    }

    ASAPChunkStorageFS(String rootDirectory, String format, int era) {
        this.rootDirectory = rootDirectory;
        this.format = format;
        this.era = era;
    }

    public String getFormat() {
        return this.format;
    }

    public String getRootDirectory() {
        return this.rootDirectory;
    }

    @Override
    public ASAPChunk getChunk(CharSequence uriTarget, int era) throws IOException {
        return new ASAPChunkFS(this, uriTarget.toString(), era);
    }

    @Override
    public boolean existsChunk(CharSequence uri, int era) throws IOException {
        String fullContentFileName = this.getChunkContentFilename(era, uri);

        boolean exists = (new File(fullContentFileName).exists());
        return exists;
    }

    String getChunkContentFilename(int era, CharSequence uri) {
        return this.getChunkFileTrunkname(era, uri.toString()) + "." +  META_DATA_EXTENSION;
    }

    String getChunkFileTrunkname(int era, String uri) {
        return this.getPath(era) + "/" + Utils.url2FileName(uri);
    }

    /**
     * 
     * @param era
     * @param targetUrl
     * @return full name (path/name) of that given url and target. Directories
     * are created if necessary.
     */
    String setupChunkFolder(int era, String targetUrl) {
        String eraFolderString = this.getPath(era);
        File eraFolder = new File(eraFolderString);
        if(!eraFolder.exists()) {
            eraFolder.mkdirs();
        }
        
        String fileName = eraFolderString + "/" + Utils.url2FileName(targetUrl);
        return fileName;
    }

    /**
     * 
     * @param era
     * @return full name (path/name) of that given url and target. Directories
     * are expected to be existent
     */
    String getFileNameByUri(int era, String uri) {
        return this.getPath(era) + "/" + uri;
    }
    
    private String getPath(int era) {
        return this.rootDirectory + "/" + Integer.toString(era);
    }

    @Override
    public List<ASAPChunk> getChunks(int era) throws IOException {
        List<ASAPChunk> chunkList = new ArrayList<>();
        
        File dir = new File(this.getPath(era));
        
        // can be null!
        File[] contentFileList = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String fileName) {
                    return fileName.endsWith(META_DATA_EXTENSION);
                }
            });

        if(contentFileList != null) {
            for (int i = 0; i < contentFileList.length; i++) {
                String name = contentFileList[i].getName();

                // cut extension
                int index = name.lastIndexOf('.');
                if(index != -1) {
                    String chunkName = name.substring(0, index);
                    String fName = this.getFileNameByUri(era, chunkName);
                    chunkList.add(new ASAPChunkFS(this, fName));
                }
            }
        }
        
        return chunkList;
    }

    @Override
    public void dropChunks(int era) throws IOException {
        // here comes a Java 6 compatible version - fits to android SDK 23
        String eraPathName = this.rootDirectory + "/" + Integer.toString(era);

        ASAPEngineFS.removeFolder(eraPathName);
    }

    @Override
    public ASAPMessages getASAPMessages(CharSequence uri, int toEra) throws IOException {
        // INIT ++++++++++++++++++++++ toEra +++++++++++++++++++++ MAX

        int fromEra = ASAP.nextEra(toEra); // the whole cycle
        return this.getASAPMessages(uri, fromEra, toEra);
    }

    @Override
    public ASAPMessages getASAPMessages(CharSequence uri, int fromEra, int toEra) throws IOException {
        Log.writeLog(this, "create ASAPInMemoMessages");

        return new ASAPInMemoMessages(
                this,
                this.getFormat(),
                uri,
                fromEra, // set starting era
                toEra // anything before
        );
    }

    @Override
    public ASAPMessages getASAPMessages(String uri) throws ASAPException, IOException {
        if(this.era == -1) {
            throw new ASAPException("internal error: era not set - use other constructor or method");
        }
        return this.getASAPMessages(uri, this.era);
    }
}
