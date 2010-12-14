package org.apache.aries.spifly.impl2;

import org.apache.aries.mytest.MySPI;

public class MySPIImpl2 implements MySPI{
    @Override
    public String someMethod(String s) {
        return s.toUpperCase();
    }
}
