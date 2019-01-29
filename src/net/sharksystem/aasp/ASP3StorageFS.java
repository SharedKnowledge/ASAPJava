package net.sharksystem.aasp;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static net.sharksystem.aasp.ASP3ChunkFS.DATA_EXTENSION;

/**
 *
 * @author thsc
 */
class ASP3StorageFS implements ASP3Storage {

    private final String rootDirectory;

    ASP3StorageFS(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public ASP3Chunk getChunk(CharSequence uriTarget, int era) throws IOException {
        return new ASP3ChunkFS(this, (String) uriTarget, era);
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
     * @param fileName
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
    public List<ASP3Chunk> getChunks(int era) throws IOException {
        List<ASP3Chunk> chunkList = new ArrayList<>();
        
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
                    chunkList.add(new ASP3ChunkFS(this, fName));
                }
            }
        }
        
        return chunkList;
    }

    @Override
    public void dropChunks(int era) throws IOException {
        // here comes a Java 6 compatible version - fits to android SDK 23
        String eraPathName = this.rootDirectory + "/" + Integer.toString(era); 
        File dir = new File(eraPathName);
        
        String[] dirEntries = dir.list();
        
        /*
        Path dir = Paths.get(this.rootDirectory + "/" + Integer.toString(era));
        
        DirectoryStream<Path> entries = null;
        try {
            entries = Files.newDirectoryStream(dir);
        }
        catch(IOException ioe) {
            // directory does not exist - ok, nothing to drop
            return;
        }
        
        for (Path path : entries) {
            File file = path.toFile();
            file.delete();
        }
*/
        if(dirEntries != null) {
            for(String fileName : dirEntries) {
                File fileInDir = new File(eraPathName + "/" + fileName);
                try {
                    fileInDir.delete();
                }
                catch(RuntimeException e) {
                    System.err.println("AASPStorageFS: cannot remove old file:" + e.getLocalizedMessage());
                    // try next
                }
            }
        }
        
        // finally remove directory itself
//        dir.toFile().delete();

        dir.delete();
    }

    String getRootPath() {
        return this.rootDirectory;
    }
}
