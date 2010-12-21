package org.apache.aries.spifly;

import java.util.ServiceLoader;

import org.apache.aries.mytest.MySPI;

public class UnaffectedTestClient {
    public String test(String input) {
        StringBuilder sb = new StringBuilder();
        
        ServiceLoader<MySPI> loader = ServiceLoader.load(MySPI.class,
            new ClientWeavingHookTest.TestImplClassLoader("impl4", "META-INF/services/org.apache.aries.mytest.MySPI"));
        for (MySPI mySPI : loader) {
            sb.append(mySPI.someMethod(input));
        }
        return sb.toString();
    }
}
