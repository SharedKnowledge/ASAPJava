package net.sharksystem;

import net.sharksystem.asap.crypto.CryptoUsage;
import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.helper.HelperTester;
import net.sharksystem.asap.protocol.PDUTests;
import net.sharksystem.asap.serialization.SerializationTests;
import net.sharksystem.asap.storage.StorageTests;
import net.sharksystem.streams.E2EStreamPairLinkTestVersion2;
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
        E2EStreamPairLinkTestVersion2.class
})
public class V1TestSuite {

}
