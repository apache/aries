package org.apache.aries.transaction.pojo;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Transactional(value=TxType.MANDATORY)
public class ExtendedPojo3 extends BaseClass {

    @Override
    public void defaultType(String test) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1382
        super.defaultType(test);
    }

}
