package org.apache.aries.functional;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function9<A,B,C,D,E,F,G,H,I,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d,E e,F f,G g,H h,I i);
    
    default public Function<A,Function<B,Function<C,Function<D,Function<E,Function<F,Function<G,Function<H,Function<I,RESULT>>>>>>>>> curried() {
        return a -> b -> c -> d -> e -> f -> g -> h -> i -> apply(a,b,c,d,e,f,g,h,i);
    }
}
