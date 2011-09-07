package beans.integration;

import javax.ejb.Local;

@Local
public interface Tx {
  public Object getNoTransactionId() throws Exception;
  public Object getMaybeTransactionId() throws Exception;
  public Object getTransactionId() throws Exception;
  public Object getNewTransactionId() throws Exception;
}
