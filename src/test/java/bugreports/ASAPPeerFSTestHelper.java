package bugreports;

import java.io.File;

public class ASAPPeerFSTestHelper{

    private CharSequence rootdir, format;

    public ASAPPeerFSTestHelper(CharSequence rootdir, CharSequence format) {
        this.rootdir = rootdir;
        this.format = format;
    }

    /**
     * Checks if the content and meta files of the given info exists
     *
     * @param owner  owner of the folder
     * @param sender sender of a message
     * @param uri    uri of a message
     * @param era    era of a message
     * @return true if files exist, else false
     */
    public boolean senderEraShouldExist(String owner, String sender, String uri, int era) {
        String path = rootdir + "/" + owner +
                "/" + format + "/" + sender + "/" +
                era + "/" + uri;
        String pathToMetaFile = path + ".meta";
        String pathToContentFile = path + ".content";
        System.out.println("PATH: " + path);
        return new File(pathToMetaFile).exists() && new File(pathToContentFile).exists();
    }
}
