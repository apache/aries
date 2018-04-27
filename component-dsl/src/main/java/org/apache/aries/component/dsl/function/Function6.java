package org.apache.aries.component.dsl.function;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function6<A,B,C,D,E,F,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d,E e,F f);
    
    default public Function<A,Function<B,Function<C,Function<D,Function<E,Function<F,RESULT>>>>>> curried() {
        return a -> b -> c -> d -> e -> f -> apply(a,b,c,d,e,f);
    }
}
