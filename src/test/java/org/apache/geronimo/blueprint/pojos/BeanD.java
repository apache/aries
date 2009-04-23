package org.apache.geronimo.blueprint.pojos;

public class BeanD {

    public static Runnable run;

    public void init() {
        run.run();
    }

}
