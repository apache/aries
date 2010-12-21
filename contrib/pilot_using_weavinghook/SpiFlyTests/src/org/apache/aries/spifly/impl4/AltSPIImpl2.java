package org.apache.aries.spifly.impl4;

import org.apache.aries.mytest.AltSPI;

public class AltSPIImpl2 implements AltSPI {
    @Override
    public long square(long l) {
        return -l * l;
    }
}
