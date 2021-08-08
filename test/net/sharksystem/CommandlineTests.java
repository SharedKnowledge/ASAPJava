package net.sharksystem;

import net.sharksystem.utils.Commandline;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class CommandlineTests {
    @Test
    public void test1() {
        String key0 = "-help";
        String key1 = "-o";
        String value1 = "xyz";
        String[] args = new String[] {key0, key1, value1};
        HashMap<String, String> argsMap = Commandline.parametersToMap(args, false, "helpmessage");

        Assert.assertEquals(2, argsMap.size());

        // 0
        Assert.assertNull(argsMap.get(key0));
        // 1
        String valueInMap = argsMap.get(key1);
        Assert.assertTrue(valueInMap.equalsIgnoreCase(value1));
    }
}
