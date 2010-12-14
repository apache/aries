package org.apache.aries.spifly;

import java.util.ServiceLoader;

import org.apache.aries.mytest.MySPI;

public class TestClient {
    public String test(String input) {
        StringBuilder sb = new StringBuilder();
        
        ServiceLoader<MySPI> loader = ServiceLoader.load(MySPI.class);
        for (MySPI mySPI : loader) {
            sb.append(mySPI.someMethod(input));
        }
        return sb.toString();
    }
}
