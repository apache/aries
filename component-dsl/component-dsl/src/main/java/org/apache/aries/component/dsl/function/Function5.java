package org.apache.aries.component.dsl.function;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function5<A,B,C,D,E,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d,E e);
    
    default public Function<A,Function<B,Function<C,Function<D,Function<E,RESULT>>>>> curried() {
        return a -> b -> c -> d -> e -> apply(a,b,c,d,e);
    }
}
