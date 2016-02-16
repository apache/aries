package org.apache.aries.tx.control.service.local.impl;

import static org.osgi.service.transaction.control.TransactionStatus.COMMITTED;
import static org.osgi.service.transaction.control.TransactionStatus.COMMITTING;
import static org.osgi.service.transaction.control.TransactionStatus.MARKED_ROLLBACK;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLED_BACK;
import static org.osgi.service.transaction.control.TransactionStatus.ROLLING_BACK;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.transaction.xa.XAResource;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionStatus;

public class TransactionContextImpl extends AbstractTransactionContextImpl
		implements TransactionContext {

	final List<LocalResource>				resources			= new ArrayList<>();

	final List<Runnable>					preCompletion		= new ArrayList<>();
	final List<Consumer<TransactionStatus>>	postCompletion		= new ArrayList<>();

	private volatile TransactionStatus		tranStatus;

	public TransactionContextImpl(Coordination coordination) {
		super(coordination);

		tranStatus = TransactionStatus.ACTIVE;

		coordination.addParticipant(new Participant() {

			@Override
			public void failed(Coordination coordination) throws Exception {
				setRollbackOnly();

				beforeCompletion();

				vanillaRollback();

				afterCompletion();
			}

			private void vanillaRollback() {

				tranStatus = ROLLING_BACK;

				resources.stream().forEach(lr -> {
					try {
						lr.rollback();
					} catch (Exception e) {
						// TODO log this
					}
				});

				tranStatus = ROLLED_BACK;
			}

			private void beforeCompletion() {
				preCompletion.stream().forEach(r -> {
					try {
						r.run();
					} catch (Exception e) {
						unexpectedException.compareAndSet(null, e);
						setRollbackOnly();
						// TODO log this
					}
				});
			}

			private void afterCompletion() {
				postCompletion.stream().forEach(c -> {
					try {
						c.accept(tranStatus);
					} catch (Exception e) {
						unexpectedException.compareAndSet(null, e);
						// TODO log this
					}
				});
			}

			@Override
			public void ended(Coordination coordination) throws Exception {
				beforeCompletion();

				if (getRollbackOnly()) {
					vanillaRollback();
				} else {
					tranStatus = COMMITTING;

					List<LocalResource> committed = new ArrayList<>(
							resources.size());
					List<LocalResource> rolledback = new ArrayList<>(0);

					resources.stream().forEach(lr -> {
						try {
							if (getRollbackOnly()) {
								lr.rollback();
								rolledback.add(lr);
							} else {
								lr.commit();
								committed.add(lr);
							}
						} catch (Exception e) {
							unexpectedException.compareAndSet(null, e);
							if (committed.isEmpty()) {
								tranStatus = ROLLING_BACK;
							}
							rolledback.add(lr);
						}
					});
					tranStatus = tranStatus == ROLLING_BACK ? ROLLED_BACK
							: COMMITTED;
				}
				afterCompletion();
			}
		});
	}

	@Override
	public Object getTransactionKey() {
		return coordination.getId();
	}

	@Override
	public boolean getRollbackOnly() throws IllegalStateException {
		switch (tranStatus) {
			case MARKED_ROLLBACK :
			case ROLLING_BACK :
			case ROLLED_BACK :
				return true;
			default :
				return false;
		}
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException {
		switch (tranStatus) {
			case ACTIVE :
			case MARKED_ROLLBACK :
				tranStatus = MARKED_ROLLBACK;
				break;
			case COMMITTING :
				// TODO something here? If it's the first resource then it might
				// be ok to roll back?
				throw new IllegalStateException(
						"The transaction is already being committed");
			case COMMITTED :
				throw new IllegalStateException(
						"The transaction is already committed");

			case ROLLING_BACK :
			case ROLLED_BACK :
				// A no op
				break;
			default :
				throw new IllegalStateException(
						"The transaction is in an unkown state");
		}
	}

	@Override
	public TransactionStatus getTransactionStatus() {
		return tranStatus;
	}

	@Override
	public void preCompletion(Runnable job) throws IllegalStateException {
		if (tranStatus.compareTo(MARKED_ROLLBACK) > 0) {
			throw new IllegalStateException(
					"The current transaction is in state " + tranStatus);
		}

		preCompletion.add(job);
	}

	@Override
	public void postCompletion(Consumer<TransactionStatus> job)
			throws IllegalStateException {
		if (tranStatus == COMMITTED || tranStatus == ROLLED_BACK) {
			throw new IllegalStateException(
					"The current transaction is in state " + tranStatus);
		}

		postCompletion.add(job);
	}

	@Override
	public void registerXAResource(XAResource resource) {
		throw new IllegalStateException("Not an XA manager");
	}

	@Override
	public void registerLocalResource(LocalResource resource) {
		if (tranStatus.compareTo(MARKED_ROLLBACK) > 0) {
			throw new IllegalStateException(
					"The current transaction is in state " + tranStatus);
		}
		resources.add(resource);
	}

	@Override
	public boolean supportsXA() {
		return false;
	}

	@Override
	public boolean supportsLocal() {
		return true;
	}
}
