package org.apache.aries.spifly;

import java.util.ServiceLoader;

import org.apache.aries.mytest.AltSPI;

public class AltTestClient {
    public long test(long input) {
        long result = 0;
        
        ServiceLoader<AltSPI> loader = ServiceLoader.load(AltSPI.class);
        for (AltSPI mySPI : loader) {
            result += mySPI.square(input);
        }
        return result;
    }
}
