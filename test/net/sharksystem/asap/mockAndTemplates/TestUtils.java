package net.sharksystem.asap.mockAndTemplates;

import java.io.*;

public class TestUtils {
    static final CharSequence ALICE = "Alice";
    static final CharSequence BOB = "Bob";
    static final CharSequence YOUR_APP_NAME = "yourAppName";
    static final CharSequence YOUR_URI = "yourSchema://example";

    /**
     * a serialization example
     * @param exampleLong
     * @param exampleString
     * @param exampleBoolean
     * @return
     */
    public static byte[] serializeExample(long exampleLong, String exampleString, boolean exampleBoolean) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);

        // serialize
        daos.writeLong(exampleLong);
        daos.writeUTF(exampleString);
        daos.writeBoolean(exampleBoolean);

        return baos.toByteArray();
    }

    /**
     * a deserialization example
     */
    public static void deserializeExample(byte[] serializedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
        DataInputStream dais = new DataInputStream(bais);

        // deserialize
        long exampleLong = dais.readLong();
        String exampleString = dais.readUTF();
        boolean exampleBoolean = dais.readBoolean();

        // call a methode in your app

        // here: just print
        System.out.println("received: " + exampleLong + " | " + exampleString + " | " + exampleBoolean);
    }
}
