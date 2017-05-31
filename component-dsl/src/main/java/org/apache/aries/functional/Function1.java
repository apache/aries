package org.apache.aries.functional;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function1<A,RESULT> {
    
    public RESULT apply(A a);
    
    default public Function<A,RESULT> curried() {
        return a -> apply(a);
    }
}
