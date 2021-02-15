package net.sharksystem.asap.serialization;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerializationTests {
    @Test
    public void test1() throws IOException, ASAPException {
        String messageIn = "I am Alice";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeCharSequenceParameter(messageIn, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        String messageOut = ASAPSerialization.readCharSequenceParameter(bais);

        Assert.assertTrue(messageOut.equals(messageIn));
    }
}
