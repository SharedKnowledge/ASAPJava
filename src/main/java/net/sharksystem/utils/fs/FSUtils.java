package net.sharksystem.utils.fs;

import net.sharksystem.utils.Log;

import java.io.File;

public class FSUtils {
    ////////////////////////////////////////////////////////////////////////////////////////
    //                                         helper                                     //
    ////////////////////////////////////////////////////////////////////////////////////////

    public static void removeFolder(String eraPathName) {
        File dir = new File(eraPathName);

        String[] dirEntries = dir.list();

        if(dirEntries != null) {
            for(String fileName : dirEntries) {
                File fileInDir = new File(eraPathName + "/" + fileName);
                if(fileInDir.isDirectory()) {
                    FSUtils.removeFolder(fileInDir.getAbsolutePath());
                } else {
                    try {
                        if(!fileInDir.delete()) {
                            Log.writeLog(FSUtils.class,"ASAPEngineFS: cannot delete file (try deleteOnExit):"
                                    + fileInDir);
                        }
                    } catch (RuntimeException e) {
                        Log.writeLog(FSUtils.class, "cannot file:" + e.getLocalizedMessage());
                        // try next
                    }
                }
            }
        }

        dir.delete();
        dir.deleteOnExit();
        try {
            Thread.sleep(1); // give file system a moment
        } catch (InterruptedException e) {
            // nobody wants to know
        }
    }
}
