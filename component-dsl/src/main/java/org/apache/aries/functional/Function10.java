package org.apache.aries.functional;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function10<A,B,C,D,E,F,G,H,I,J,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j);
    
    default public Function<A,Function<B,Function<C,Function<D,Function<E,Function<F,Function<G,Function<H,Function<I,Function<J,RESULT>>>>>>>>>> curried() {
        return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> apply(a,b,c,d,e,f,g,h,i,j);
    }
}
