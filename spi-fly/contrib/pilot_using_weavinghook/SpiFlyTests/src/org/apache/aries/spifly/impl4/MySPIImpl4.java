package org.apache.aries.spifly.impl4;

import org.apache.aries.mytest.MySPI;

public class MySPIImpl4 implements MySPI {
    @Override
    public String someMethod(String s) {
        return "impl4";
    }
}
