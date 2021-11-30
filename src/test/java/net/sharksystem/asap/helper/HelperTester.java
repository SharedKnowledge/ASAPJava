package net.sharksystem.asap.helper;

import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.asap.utils.Helper;
import org.junit.Test;

public class HelperTester {
    @Test
    public void short2bytes() {
        long valueL = System.currentTimeMillis();

        byte[] result = Helper.long2byteArray(valueL);

        ASAPSerialization.printByteArray(result);
    }
}
