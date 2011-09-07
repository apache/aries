package beans.integration.impl;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.transaction.TransactionSynchronizationRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import beans.integration.Tx;

@Singleton
public class TxSingleton implements Tx {

  private TransactionSynchronizationRegistry getTSR() {
    BundleContext ctx = FrameworkUtil.getBundle(TxSingleton.class).getBundleContext();
    return (TransactionSynchronizationRegistry) ctx.getService(
        ctx.getServiceReference("javax.transaction.TransactionSynchronizationRegistry"));
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Object getNoTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }
  
  @TransactionAttribute(TransactionAttributeType.SUPPORTS)
  public Object getMaybeTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Object getTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Object getNewTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }
}
