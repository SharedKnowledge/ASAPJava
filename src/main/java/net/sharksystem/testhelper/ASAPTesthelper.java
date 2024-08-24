package net.sharksystem.testhelper;

import net.sharksystem.utils.Log;
import net.sharksystem.utils.testsupport.TestHelper;

public class ASAPTesthelper {
    public static final String ALICE_ID = "Alice_ID";
    public static final String ALICE_NAME = "Alice";
    public static final String BOB_ID = "Bob_ID";
    public static final String BOB_NAME = "Bob";
    public static final String CLARA_ID = "Clara_ID";
    public static final String CLARA_NAME = "Clara";
    public static final String DAVID_ID = "David_ID";
    public static final String DAVID_NAME = "David";

    public static final String ROOT_DIRECTORY_TESTS = "playground/";

    public static int testNumber = 0;

    /**
     * Create a fresh testNumber
     */
    public static void incrementTestNumber() {
        testNumber++;
    }

    private static int portnumber = 7000;
    /**
     * Create a fresh TCP port.
     * @return
     */
    public static int getPortNumber() {
        portnumber++;
        return portnumber;
    }

    public static final String getUniqueFolderName(String prefix) {
        Log.writeLog(TestHelper.class, "test number == " + TestHelper.testNumber);
        return prefix + "_" + ASAPTesthelper.testNumber;
    }
}
