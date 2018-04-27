package org.apache.aries.component.dsl.function;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function8<A,B,C,D,E,F,G,H,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d,E e,F f,G g,H h);
    
    default public Function<A,Function<B,Function<C,Function<D,Function<E,Function<F,Function<G,Function<H,RESULT>>>>>>>> curried() {
        return a -> b -> c -> d -> e -> f -> g -> h -> apply(a,b,c,d,e,f,g,h);
    }
}
