package org.apache.aries.component.dsl.function;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function2<A,B,RESULT> {
    
    public RESULT apply(A a,B b);
    
    default public Function<A,Function<B,RESULT>> curried() {
        return a -> b -> apply(a,b);
    }
}
