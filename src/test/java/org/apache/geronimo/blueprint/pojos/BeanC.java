package org.apache.geronimo.blueprint.pojos;

public class BeanC {

    public static Runnable run;

    public void init() {
        run.run();
    }
}
