package org.apache.aries.functional;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function3<A,B,C,RESULT> {
    
    public RESULT apply(A a,B b,C c);
    
    default public Function<A,Function<B,Function<C,RESULT>>> curried() {
        return a -> b -> c -> apply(a,b,c);
    }
}
