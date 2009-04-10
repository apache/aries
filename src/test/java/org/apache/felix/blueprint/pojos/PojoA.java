package org.apache.felix.blueprint.pojos;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 26, 2009
 * Time: 6:16:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class PojoA implements InterfaceA {

    private PojoB pojob;

    public PojoA() {
    }

    public PojoA(PojoB pojob) {
        this.pojob = pojob;
    }

    public PojoB getPojob() {
        return pojob;
    }

    public void setPojob(PojoB pojob) {
        this.pojob = pojob;
    }
}
