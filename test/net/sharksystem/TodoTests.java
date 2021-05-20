package net.sharksystem;


import net.sharksystem.asap.crypto.CryptoUsage;
import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.helper.HelperTester;
import net.sharksystem.asap.protocol.PDUTests;
import net.sharksystem.asap.serialization.SerializationTests;
import net.sharksystem.asap.storage.StorageTests;
import net.sharksystem.components.SharkComponentTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Point2PointTests.class
})
public class TodoTests {
}
