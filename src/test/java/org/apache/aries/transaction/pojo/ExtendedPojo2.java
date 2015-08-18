package org.apache.aries.transaction.pojo;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Transactional(value=TxType.MANDATORY)
public class ExtendedPojo2 extends BaseClass {

    @Override
    public void defaultType(String test) {
    }

    @Transactional(value=TxType.SUPPORTS)
    @Override
    public void supports(String test) {
    }

}
