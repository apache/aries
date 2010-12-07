package org.apache.aries.spifly;

import org.apache.aries.mytest.MySPI;

public class MySPIImpl implements MySPI {
    @Override
    public String someMethod(String s) {
        return new StringBuilder(s).reverse().toString();
    }
}
