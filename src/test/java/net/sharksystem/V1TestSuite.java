package net.sharksystem;

import howto.ConnectPeers;
import net.sharksystem.asap.crypto.CryptoUsage;
import junit5Tests.release_1.MultipleEncounterTests;
import net.sharksystem.asap.encounter.EncounterManagerTests;
import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.helper.HelperTester;
import net.sharksystem.asap.peer.Point2Point2Test2;
import net.sharksystem.asap.peer.TransientMessages;
import net.sharksystem.asap.protocol.PDUTests;
import net.sharksystem.asap.serialization.SerializationTests;
import net.sharksystem.asap.storage.StorageTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        SerializationTests.class,
        BasisMethodsTests.class,
        UsageExamples.class,
        CreateNewChannelFromOutsideTest.class,
        PDUTests.class,
        CryptoTests.class,
        StorageTests.class,
        LongerMessages.class,
        CryptoUsage.class,
        HelperTester.class,
        MultihopTests.class,
        CommandlineTests.class,
        TransientMessages.class,
        Point2Point2Test2.class,
        EncounterManagerTests.class,
        ConnectPeers.class, //
        //MultipleEncounterTests.class
        //E2EStreamPairLinkTestVersion2.class TODO
})
public class V1TestSuite {

}
