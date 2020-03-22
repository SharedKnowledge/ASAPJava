package net.sharksystem.asap;

import org.junit.Test;

public class BasisMethodsTests {

    @Test
    public void asapID() throws InterruptedException {
        for(int i = 0; i < 10; i++) {
            System.out.println(ASAP.createUniqueID());
            Thread.sleep(2);
        }
    }
}
