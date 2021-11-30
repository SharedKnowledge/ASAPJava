package net.sharksystem.utils;

import java.util.HashMap;

public class Commandline {
    public static HashMap<String, String> parametersToMap(String args[], boolean valueRequired, String helpMessage) {
        if(valueRequired && args.length % 2 != 0) {
            System.err.println("malformed parameter list: each parameter needs a value. ");
            System.err.println(helpMessage);
            return null;
        }

        HashMap<String, String> argumentMap = new HashMap<>();

        int i = 0;
        while(i < args.length) {
            // key is followed by value. Key starts with -
            if(!args[i].startsWith("-")) {
                /* found parameter that does not start with '-'
                maybe shell parameters. Leave it alone. We are done here
                */
                return argumentMap;
            }

            // value can be empty
            if(args.length > i+1 && !args[i+1].startsWith("-")) {
                // it is a value
                argumentMap.put(args[i], args[i+1]);
                i += 2;
            } else {
                // no value - next parameter
                argumentMap.put(args[i], null);
                i += 1;
            }
        }

        return argumentMap;
    }


}
