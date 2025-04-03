package net.sharksystem.asap.serialization;

import net.sharksystem.asap.ASAPHopImpl;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPEncounterConnectionType;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.asap.utils.PeerIDHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void serializeASAPHopListLen1() throws IOException, ASAPException {
        List<ASAPHop> exampleList = new ArrayList<>();

        exampleList.add(new ASAPHopImpl("Alice", true, true, ASAPEncounterConnectionType.ASAP_HUB));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeASAPHopList(exampleList, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        List<ASAPHop> receivedList = ASAPSerialization.readASAPHopList(bais);

        Assert.assertEquals(exampleList.size(), receivedList.size());
        for(int i = 0; i < exampleList.size(); i++) {
            ASAPHop origHop = exampleList.get(i);
            ASAPHop receivedHop = receivedList.get(i);

            Assert.assertTrue(PeerIDHelper.sameID(origHop.sender(), receivedHop.sender()));
            Assert.assertTrue(origHop.verified() == receivedHop.verified());
            Assert.assertTrue(origHop.encrypted() == receivedHop.encrypted());
            Assert.assertTrue(origHop.getConnectionType() == receivedHop.getConnectionType());
        }
    }

    @Test
    public void serializeASAPHopListLen5() throws IOException, ASAPException {
        List<ASAPHop> exampleList = new ArrayList<>();

        exampleList.add(new ASAPHopImpl("Alice", true, true, ASAPEncounterConnectionType.ASAP_HUB));
        exampleList.add(new ASAPHopImpl("Bob", false, true, ASAPEncounterConnectionType.INTERNET));
        exampleList.add(new ASAPHopImpl("Clara", false, false, ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK));
        exampleList.add(new ASAPHopImpl("David", true, false, ASAPEncounterConnectionType.ONION_NETWORK));
        exampleList.add(new ASAPHopImpl("Eveline", true, false, ASAPEncounterConnectionType.UNKNOWN));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeASAPHopList(exampleList, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        List<ASAPHop> receivedList = ASAPSerialization.readASAPHopList(bais);

        Assert.assertEquals(exampleList.size(), receivedList.size());
        for(int i = 0; i < exampleList.size(); i++) {
            ASAPHop origHop = exampleList.get(i);
            ASAPHop receivedHop = receivedList.get(i);

            Assert.assertTrue(PeerIDHelper.sameID(origHop.sender(), receivedHop.sender()));
            Assert.assertTrue(origHop.verified() == receivedHop.verified());
            Assert.assertTrue(origHop.encrypted() == receivedHop.encrypted());
            Assert.assertTrue(origHop.getConnectionType() == receivedHop.getConnectionType());
        }
    }
}
