package net.sharksystem;

import net.sharksystem.asap.crypto.CryptoUsage;
import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.protocol.PDUTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        BatchprocessorTest.class,
        BasisMethodsTests.class,
        Point2PointTests.class,
        Point2PointTests2.class,
        UsageExamples.class,
        CreateNewChannelFromOutsideTest.class,
        PDUTests.class,
        CryptoTests.class,
        //SNMessageASAPSerializationTests.class,
        LongerMessages.class,
        CryptoUsage.class
})
public class V1TestSuite {

}
