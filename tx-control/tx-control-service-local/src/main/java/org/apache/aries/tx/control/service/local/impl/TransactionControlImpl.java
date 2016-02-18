package org.apache.aries.tx.control.service.local.impl;

import static java.util.Optional.ofNullable;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.concurrent.Callable;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.transaction.control.TransactionBuilder;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.TransactionRolledBackException;

public class TransactionControlImpl implements TransactionControl {

	private final class TransactionBuilderImpl extends TransactionBuilder {


		@Override
		public <T> T required(Callable<T> work)
				throws TransactionException, TransactionRolledBackException {

			Coordination currentCoord = coordinator.peek();
			boolean endCoordination = false;

			AbstractTransactionContextImpl currentTran = ofNullable(
					currentCoord).map(c -> (AbstractTransactionContextImpl) c
							.getVariables().get(TransactionContextKey.class))
							.filter(atc -> atc
									.getTransactionStatus() != NO_TRANSACTION)
							.orElse(null);
			try {
				if (currentTran == null) {
					// We must create a new coordination to scope our new
					// transaction
					currentCoord = coordinator.begin(
							"Resource-Local-Transaction.REQUIRED", 30000);
					endCoordination = true;
					currentTran = new TransactionContextImpl(currentCoord);
					currentCoord.getVariables().put(TransactionContextKey.class,
							currentTran);
				}
			} catch (RuntimeException re) {
				if (endCoordination) {
					currentCoord.end();
				}
				throw re;
			}

			return doWork(work, currentCoord, endCoordination);
		}

		@Override
		public <T> T requiresNew(Callable<T> work)
				throws TransactionException, TransactionRolledBackException {
			Coordination currentCoord = null;
			try {
				currentCoord = coordinator.begin(
						"Resource-Local-Transaction.REQUIRES_NEW", 30000);

				AbstractTransactionContextImpl currentTran = new TransactionContextImpl(
						currentCoord);
				currentCoord.getVariables().put(TransactionContextKey.class,
						currentTran);
			} catch (RuntimeException re) {
				if (currentCoord != null)
					currentCoord.end();
				throw re;
			}

			return doWork(work, currentCoord, true);
		}

		@Override
		public <T> T supports(Callable<T> work) throws TransactionException {
			Coordination currentCoord = coordinator.peek();
			boolean endCoordination = false;

			AbstractTransactionContextImpl currentTran = ofNullable(
					currentCoord).map(c -> (AbstractTransactionContextImpl) c
							.getVariables().get(TransactionContextKey.class))
							.orElse(null);
			try {
				if (currentTran == null) {
					// We must create a new coordination to scope our new
					// transaction
					currentCoord = coordinator.begin(
							"Resource-Local-Transaction.SUPPORTS", 30000);
					endCoordination = true;
					currentTran = new NoTransactionContextImpl(currentCoord);
					currentCoord.getVariables().put(TransactionContextKey.class,
							currentTran);
				}
			} catch (RuntimeException re) {
				if (endCoordination) {
					currentCoord.end();
				}
				throw re;
			}

			return doWork(work, currentCoord, endCoordination);
		}

		@Override
		public <T> T notSupported(Callable<T> work)
				throws TransactionException {
			Coordination currentCoord = coordinator.peek();
			boolean endCoordination = false;

			AbstractTransactionContextImpl currentTran = ofNullable(
					currentCoord).map(c -> (AbstractTransactionContextImpl) c
							.getVariables().get(TransactionContextKey.class))
							.filter(atc -> atc
									.getTransactionStatus() == NO_TRANSACTION)
							.orElse(null);
			try {
				if (currentTran == null) {
					// We must create a new coordination to scope our new
					// transaction
					currentCoord = coordinator.begin(
							"Resource-Local-Transaction.NOT_SUPPORTED", 30000);
					endCoordination = true;
					currentTran = new NoTransactionContextImpl(currentCoord);
					currentCoord.getVariables().put(TransactionContextKey.class,
							currentTran);
				}
			} catch (RuntimeException re) {
				if (endCoordination) {
					currentCoord.end();
				}
				throw re;
			}
			return doWork(work, currentCoord, endCoordination);
		}

		private <R> R doWork(Callable<R> transactionalWork,
				Coordination currentCoord, boolean endCoordination) {
			try {
				R result = transactionalWork.call();

				if (endCoordination) {
					currentCoord.end();
				}
				return result;
			} catch (Throwable t) {
				//TODO handle noRollbackFor
				currentCoord.fail(t);
				if (endCoordination) {
					try {
						currentCoord.end();
					} catch (CoordinationException ce) {
						if(ce.getType() == CoordinationException.FAILED) {
							throw new TransactionRolledBackException("The transaction was rolled back due to a failure", ce.getCause());
						} else {
							throw ce;
						}
					}
				}
				TransactionControlImpl.<RuntimeException> throwException(t);
			}
			throw new TransactionException(
					"The code here should never be reached");
		}
	}

	private static class TransactionContextKey {}

	private final Coordinator coordinator;

	public TransactionControlImpl(Coordinator c) {
		coordinator = c;
	}

	@Override
	public TransactionBuilder build() {
		return new TransactionBuilderImpl();
	}

	@Override
	public boolean getRollbackOnly() throws IllegalStateException {
		return getCurrentTranContextChecked().getRollbackOnly();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException {
		getCurrentTranContextChecked().setRollbackOnly();
	}

	@Override
	public <T> T required(Callable<T> work)
			throws TransactionException, TransactionRolledBackException {
		return build().required(work);
	}

	@Override
	public <T> T requiresNew(Callable<T> work)
			throws TransactionException, TransactionRolledBackException {
		return build().requiresNew(work);
	}

	@Override
	public <T> T notSupported(Callable<T> work) throws TransactionException {
		return build().notSupported(work);
	}

	@Override
	public <T> T supports(Callable<T> work) throws TransactionException {
		return build().supports(work);
	}

	@Override
	public boolean activeTransaction() {
		TransactionContext context = getCurrentContext();
		return context != null
				&& context.getTransactionStatus() != NO_TRANSACTION;
	}

	@Override
	public boolean activeScope() {
		return getCurrentContext() != null;
	}

	private TransactionContext getCurrentTranContextChecked() {
		TransactionContext toUse = getCurrentContext();
		if (toUse == null) {
			throw new IllegalStateException(
					"There is no applicable transaction context");
		}
		return toUse;
	}

	@Override
	public TransactionContext getCurrentContext() {
		TransactionContext toUse = null;

		Coordination peek = coordinator.peek();
		if (peek != null) {
			toUse = (TransactionContext) peek.getVariables()
					.get(TransactionContextKey.class);
		}
		return toUse;
	}

	/**
	 * Borrowed from the netty project as a way to avoid wrapping checked
	 * exceptions Viewable at https://github.com/netty/netty/
	 * netty/common/src/main/java/io/netty/util/internal/PlatformDependent.java
	 * 
	 * @param t
	 * @return
	 * @throws T
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T throwException(Throwable t)
			throws T {
		throw (T) t;
	}

	@Override
	public void ignoreException(Throwable t) throws IllegalStateException {
		// TODO Auto-generated method stub

	}

}
