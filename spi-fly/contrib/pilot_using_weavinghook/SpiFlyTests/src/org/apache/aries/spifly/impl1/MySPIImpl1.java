package org.apache.aries.spifly.impl1;

import org.apache.aries.mytest.MySPI;

public class MySPIImpl1 implements MySPI{
    @Override
    public String someMethod(String s) {
        return new StringBuilder(s).reverse().toString();
    }
}
