package net.sharksystem.aasp;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static net.sharksystem.aasp.AASPChunkFS.DATA_EXTENSION;

/**
 *
 * @author thsc
 */
class AASPChunkStorageFS implements AASPChunkStorage {

    private final String rootDirectory;

    AASPChunkStorageFS(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public AASPChunk getChunk(CharSequence uriTarget, int era) throws IOException {
        return new AASPChunkFS(this, (String) uriTarget, era);
    }
    
    String url2FileName(String url) {
        // escape:
        /*
        see https://en.wikipedia.org/wiki/Percent-encoding
        \ - %5C, / - %2F, : - %3A, ? - %3F," - %22,< - %3C,> - %3E,| - %7C
        */
        
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
    String getFullFileName(int era, String targetUrl) {
        String eraFolderString = this.getRootPath() + "/" + Integer.toString(era);
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
    String getFullFileNameByChunkName(int era, String contentName) {
        return this.getPath(era) + "/" + contentName;
    }
    
    private String getPath(int era) {
        return this.rootDirectory + "/" + Integer.toString(era);
    }

    @Override
    public List<AASPChunk> getChunks(int era) throws IOException {
        List<AASPChunk> chunkList = new ArrayList<>();
        
        File dir = new File(this.getPath(era));
        
        // can be null!
        File[] contentFileList = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String fileName) {
                    return fileName.endsWith(DATA_EXTENSION);
                }
            });

        if(contentFileList != null) {
            for (int i = 0; i < contentFileList.length; i++) {
                String name = contentFileList[i].getName();

                // cut extension
                int index = name.lastIndexOf('.');
                if(index != -1) {
                    String chunkName = name.substring(0, index);
                    String fName = this.getFullFileNameByChunkName(era, chunkName);
                    chunkList.add(new AASPChunkFS(this, fName));
                }
            }
        }
        
        return chunkList;
    }

    @Override
    public void dropChunks(int era) throws IOException {
        // here comes a Java 6 compatible version - fits to android SDK 23
        String eraPathName = this.rootDirectory + "/" + Integer.toString(era);

        AASPEngineFS.removeFolder(eraPathName);

    }

    String getRootPath() {
        return this.rootDirectory;
    }

    @Override
    public AASPChunkCache getAASPChunkCache(CharSequence uri, int fromEra, 
            int toEra) throws IOException {
        return new AASPInMemoChunkCache(this, uri, fromEra, toEra);
    }
}
