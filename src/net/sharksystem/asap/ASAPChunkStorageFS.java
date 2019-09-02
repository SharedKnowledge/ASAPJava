package net.sharksystem.asap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static net.sharksystem.asap.ASAPChunkFS.DATA_EXTENSION;
import static net.sharksystem.asap.ASAPChunkFS.META_DATA_EXTENSION;

/**
 *
 * @author thsc
 */
class ASAPChunkStorageFS implements ASAPChunkStorage {

    private final String rootDirectory;

    ASAPChunkStorageFS(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public ASAPChunk getChunk(CharSequence uriTarget, int era) throws IOException {
        return new ASAPChunkFS(this, uriTarget.toString(), era);
    }

    @Override
    public boolean existsChunk(CharSequence uri, int era) throws IOException {
        String fullContentFileName = this.getChunkContentFilename(era, uri);

        return(new File(fullContentFileName).exists());
    }

    String getChunkContentFilename(int era, CharSequence uri) {
        return this.getChunkFileTrunkname(era, uri.toString()) + "." +  DATA_EXTENSION;
    }

    String getChunkFileTrunkname(int era, String uri) {
        return this.getPath(era) + "/" + this.url2FileName(uri);
    }

    String url2FileName(String url) {
        // escape:
        /*
        see https://en.wikipedia.org/wiki/Percent-encoding
        \ - %5C, / - %2F, : - %3A, ? - %3F," - %22,< - %3C,> - %3E,| - %7C
        */

        if(url == null) return null; // to be safe
        
        String newString = url.replace("\\", "%5C");
        newString = newString.replace("/", "%2F");
        newString = newString.replace(":", "%3A");
        newString = newString.replace("?", "%3F");
        newString = newString.replace("\"", "%22");
        newString = newString.replace("<", "%3C");
        newString = newString.replace(">", "%3E");
        newString = newString.replace("|", "%7C");
        
        return newString;
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
        
        String fileName = eraFolderString + "/" + this.url2FileName(targetUrl);
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
    public ASAPChunkCache getASAPChunkCache(CharSequence uri, int toEra) throws IOException {
        // go back 1000 eras
        int fromEra = toEra;
        for(int i = 0; i < 1000; i++) {
            fromEra = ASAPEngine.previousEra(fromEra);
        }

        return new ASAPInMemoChunkCache(this,
                uri,
                fromEra, // set starting era
                toEra // anything before
        );
    }
}
