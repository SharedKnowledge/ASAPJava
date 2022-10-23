package net.sharksystem.utils.testsupport;

public interface TestConstants {
    String ROOT_DIRECTORY = "testResultsRootFolder/";
    String ALICE_ID = "Alice_42";
    String ALICE_NAME = "Alice";
    String BOB_ID = "Bob_43";
    String BOB_NAME = "Bob";
    String CLARA_ID = "Clara_44";
    String CLARA_NAME = "Clara";
    String DAVID_ID = "David_45";
    String DAVID_NAME = "David";

    String URI = "shark://testUri";
    byte[] MESSAGE_1 = "1st message".getBytes();
    byte[] MESSAGE_2 = "2nd message".getBytes();

    byte[] MESSAGE_ALICE_TO_BOB_1 = "Alice -> Bob (#1)".getBytes();
    byte[] MESSAGE_BOB_TO_ALICE_1 = "Bob -> Alice (#1)".getBytes();
    byte[] MESSAGE_ALICE_TO_BOB_2 = "Alice -> Bob (#1)".getBytes();
    byte[] MESSAGE_BOB_TO_ALICE_2 = "Bob -> Alice (#1)".getBytes();
}
