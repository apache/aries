package org.apache.aries.component.dsl.function;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function4<A,B,C,D,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d);
    
    default public Function<A,Function<B,Function<C,Function<D,RESULT>>>> curried() {
        return a -> b -> c -> d -> apply(a,b,c,d);
    }
}
