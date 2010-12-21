package org.apache.aries.spifly.impl2;

import org.apache.aries.mytest.AltSPI;

public class AltSPIImpl1 implements AltSPI {
    @Override
    public long square(long l) {
        return l * l;
    }
}
