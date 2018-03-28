package org.apache.aries.functional;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function7<A,B,C,D,E,F,G,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d,E e,F f,G g);
    
    default public Function<A,Function<B,Function<C,Function<D,Function<E,Function<F,Function<G,RESULT>>>>>>> curried() {
        return a -> b -> c -> d -> e -> f -> g -> apply(a,b,c,d,e,f,g);
    }
}
