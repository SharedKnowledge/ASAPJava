package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BasisMethodsTests {

    @Test
    public void asapID() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            System.out.println(ASAP.createUniqueID());
            Thread.sleep(2);
        }
    }

    @Test
    public void bitMasks() throws IOException, ASAPException {
        int flags = 0x0000BA98;
        System.out.print("1: ");
        ASAPSerialization.printBits(flags);
        System.out.print("\n");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte flagBytes = ASAPSerialization.getByteFromInt(flags, 0);
        ASAPSerialization.writeByteParameter(flagBytes, os); // mand
        flagBytes = ASAPSerialization.getByteFromInt(flags, 1);
        ASAPSerialization.writeByteParameter(flagBytes, os); // mand

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        byte byteLeastSignificant = ASAPSerialization.readByte(is);
        byte byteNextByte = ASAPSerialization.readByte(is);

        int retFlags = byteNextByte;
        retFlags = retFlags << 8;
        System.out.print("2: ");
        ASAPSerialization.printBits(retFlags);
        System.out.print("\n");

        // set all random bits to 0
        retFlags = retFlags & 0x0000FF00;
        // set all bits in higher byte to zero - not exactly necessary but makes debugging easier
        System.out.print("3: ");
        ASAPSerialization.printBits(retFlags);
        System.out.print("\n");

        int leastSignificantInt = byteLeastSignificant;
        leastSignificantInt = leastSignificantInt & 0x000000FF;
        System.out.print("4: ");
        ASAPSerialization.printBits(leastSignificantInt);
        System.out.print("\n");

        retFlags = retFlags | leastSignificantInt;
        System.out.print("5: ");
        ASAPSerialization.printBits(retFlags);
        System.out.print("\n");

        Assert.assertEquals(flags, retFlags);
    }
}
