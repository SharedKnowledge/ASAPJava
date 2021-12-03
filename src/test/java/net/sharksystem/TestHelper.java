package net.sharksystem;

import net.sharksystem.utils.Utils;

public class TestHelper {
    public static int testNumber = 0;
    public static int portNumber = 4444;

    public static int getPortNumber() {
        return portNumber++;
    }

    public static String getFullTempFolderName(String fullRootFolderName, boolean increment) {
//        String retVal = fullRootFolderName + "_" + testNumber;
        String retVal = fullRootFolderName + "/test_" + testNumber;
        if(increment) testNumber++;
        return retVal;
    }

    public static String getFullRootFolderName(String peerID, Class testClass) {
        return TestConstants.ROOT_DIRECTORY
                + testClass.getSimpleName()
                + "/"
                + peerID;
    }

    public static String produceTestAppName(Class testClass) {
        return "application/x-" + testClass.getSimpleName();
    }

    public static int testMessageCounter = 0;

    /**
     * Produce a test message - just some bytes - Note: This methods return a different message
     * after each call.
     * @return
     */
    public static byte[] produceTestMessage() {
        String s = "testMessage" + testMessageCounter++;
        return s.getBytes();
    }

    public static boolean sameMessages(byte[] msgA, byte[] msgB) {
        return Utils.compareArrays(msgA, msgB);
    }
}
