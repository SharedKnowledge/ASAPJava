package net.sharksystem.asap;

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
        CryptoTests.class
})
public class V1TestSuite {

}
