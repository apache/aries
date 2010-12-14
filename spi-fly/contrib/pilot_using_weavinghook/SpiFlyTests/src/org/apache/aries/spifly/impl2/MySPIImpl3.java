package org.apache.aries.spifly.impl2;

import org.apache.aries.mytest.MySPI;

public class MySPIImpl3 implements MySPI{
    @Override
    public String someMethod(String s) {
        return "" + s.length();
    }
}
