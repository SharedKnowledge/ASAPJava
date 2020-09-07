package net.sharksystem.asap;

import net.sharksystem.asap.protocol.PDUTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        PDUTests.class,
        BatchprocessorTest.class,
        BasisMethodsTests.class,
        Point2PointTests.class,
        Point2PointTests2.class,
        UsageExamples.class,
        CreateNewChannelFromOutsideTest.class
})
public class V1TestSuite {

}
