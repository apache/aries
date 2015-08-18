package org.apache.aries.transaction.pojo;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Transactional(value=TxType.REQUIRED)
public class BaseClass {

    @Transactional(value=TxType.NEVER)
    public void defaultType(String test) {
        
    }
    
    public void supports(String test) {
        
    }
}
