package org.apache.aries.tx.control.service.local.impl;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionBuilder;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.TransactionRolledBackException;

public class TransactionControlImpl implements TransactionControl {

	private final class TransactionBuilderImpl extends TransactionBuilder {

		private void checkExceptions() {
			List<Class<? extends Throwable>> duplicates = rollbackFor.stream()
					.filter(noRollbackFor::contains)
					.collect(toList());
			if(!duplicates.isEmpty()) {
				throw new TransactionException("The transaction declares that the Exceptions " + 
						duplicates + " must both trigger and not trigger rollback");
			}
		}

		@Override
		public <T> T required(Callable<T> work)
				throws TransactionException, TransactionRolledBackException {
			checkExceptions();
			
			Coordination currentCoord = coordinator.peek();
			boolean endTransaction = false;
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
					endTransaction = true;
					currentCoord.getVariables().put(TransactionContextKey.class,
							currentTran);
				}
			} catch (RuntimeException re) {
				if(endTransaction) {
					currentTran.finish();
				}
				if (endCoordination) {
					currentCoord.end();
				}
				throw re;
			}

			return doWork(work, currentTran, currentCoord, endTransaction, endCoordination);
		}

		@Override
		public <T> T requiresNew(Callable<T> work)
				throws TransactionException, TransactionRolledBackException {
			checkExceptions();
			
			Coordination currentCoord = null;
			AbstractTransactionContextImpl currentTran;
			try {
				currentCoord = coordinator.begin(
						"Resource-Local-Transaction.REQUIRES_NEW", 30000);

				currentTran = new TransactionContextImpl(
						currentCoord);
				currentCoord.getVariables().put(TransactionContextKey.class,
						currentTran);
			} catch (RuntimeException re) {
				if (currentCoord != null)
					currentCoord.end();
				throw re;
			}

			return doWork(work, currentTran, currentCoord, true, true);
		}

		@Override
		public <T> T supports(Callable<T> work) throws TransactionException {
			checkExceptions();
			
			Coordination currentCoord = coordinator.peek();
			boolean endTransaction = false;
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
					endTransaction = true;
					currentCoord.getVariables().put(TransactionContextKey.class,
							currentTran);
				}
			} catch (RuntimeException re) {
				if(endTransaction) {
					currentTran.finish();
				}
				if (endCoordination) {
					currentCoord.end();
				}
				throw re;
			}

			return doWork(work, currentTran, currentCoord, endTransaction, endCoordination);
		}

		@Override
		public <T> T notSupported(Callable<T> work)
				throws TransactionException {
			checkExceptions();
			
			Coordination currentCoord = coordinator.peek();
			boolean endTransaction = false;
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
					endTransaction = true;
					currentCoord.getVariables().put(TransactionContextKey.class,
							currentTran);
				}
			} catch (RuntimeException re) {
				if(endTransaction) {
					currentTran.finish();
				}
				if (endCoordination) {
					currentCoord.end();
				}
				throw re;
			}
			return doWork(work, currentTran, currentCoord, endTransaction, endCoordination);
		}

		private <R> R doWork(Callable<R> transactionalWork,
				AbstractTransactionContextImpl currentTran, Coordination currentCoord, 
				boolean endTransaction, boolean endCoordination) {
			R result;
			try {
				result = transactionalWork.call();

			} catch (Throwable t) {
				//TODO handle noRollbackFor
				if(requiresRollback(t)) {
					currentCoord.fail(t);
				}
				if(endTransaction) {
					try {
						currentTran.finish();
					} catch (Exception e) {
						currentTran.recordFailure(e);
					}
				}
				if (endCoordination) {
					try {
						currentCoord.end();
					} catch (CoordinationException ce) {
						if(ce.getType() != CoordinationException.FAILED) {
							currentTran.recordFailure(ce);
						}
					}
				}
				ScopedWorkException workException = new ScopedWorkException("The scoped work threw an exception", t, 
						endCoordination ? null : currentTran);
				Throwable throwable = currentTran.firstUnexpectedException.get();
				if(throwable != null) {
					workException.addSuppressed(throwable);
				}
				currentTran.subsequentExceptions.stream().forEach(workException::addSuppressed);
				
				throw workException;
			}
			
			if(endTransaction) {
				try {
					currentTran.finish();
				} catch (Exception e) {
					currentTran.recordFailure(e);
					currentCoord.fail(e);
				}
			}
			try {
				if (endCoordination) {
					currentCoord.end();
				}
			} catch (CoordinationException ce) {
				if(ce.getType() != CoordinationException.FAILED) {
					currentTran.recordFailure(ce);
				}
			}
			
			Throwable throwable = currentTran.firstUnexpectedException.get();
			if(throwable != null) {
				TransactionException te = currentTran.getTransactionStatus() == ROLLED_BACK ?
						new TransactionRolledBackException("The transaction rolled back due to a failure", throwable) :
						new TransactionException("There was an error in the Transaction completion.", throwable);
				
				currentTran.subsequentExceptions.stream().forEach(te::addSuppressed);
				
				throw te;
			}
			
			return result;
		}

		private boolean requiresRollback(Throwable t) {
			return mostSpecificMatch(noRollbackFor, t)
				.map(noRollbackType -> mostSpecificMatch(rollbackFor, t)
						.map(rollbackType -> noRollbackType.isAssignableFrom(rollbackType))
						.orElse(false))
				.orElse(true);
		}
		
		private Optional<Class<? extends Throwable>> mostSpecificMatch(Collection<Class<? extends Throwable>> types, Throwable t) {
			return types.stream()
					.filter(c -> c.isInstance(t))
					.max((c1, c2) -> {
							if(c1 == c2) return 0;
							
							return c1.isAssignableFrom(c2) ? 1 : c2.isAssignableFrom(c1) ? -1 : 0;
						});
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

	@Override
	public void ignoreException(Throwable t) throws IllegalStateException {
		// TODO Auto-generated method stub

	}

}
