package net.sharksystem.asap;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        BatchprocessorTest.class,
        BasisMethodsTests.class,
        Point2PointTests.class,
        Point2PointTests2.class,
        UsageExamples.class,
        CreateNewChannelFromOutsideTest.class
})
public class V1TestSuite {

}
