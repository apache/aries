package org.apache.aries.tx.control.jpa.local.impl;

import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.apache.aries.tx.control.jpa.common.impl.EntityManagerWrapper;
import org.apache.aries.tx.control.jpa.common.impl.ScopedEntityManagerWrapper;
import org.apache.aries.tx.control.jpa.common.impl.TxEntityManagerWrapper;
import org.osgi.service.transaction.control.LocalResource;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;

public class TxContextBindingEntityManager extends EntityManagerWrapper {

	private final TransactionControl	txControl;
	private final UUID					resourceId;
	private final EntityManagerFactory	emf;

	public TxContextBindingEntityManager(TransactionControl txControl,
			EntityManagerFactory emf, UUID resourceId) {
		this.txControl = txControl;
		this.emf = emf;
		this.resourceId = resourceId;
	}

	@Override
	protected final EntityManager getRealEntityManager() {

		TransactionContext txContext = txControl.getCurrentContext();

		if (txContext == null) {
			throw new TransactionException("The resource " + emf
					+ " cannot be accessed outside of an active Transaction Context");
		}

		EntityManager existing = (EntityManager) txContext.getScopedValue(resourceId);

		if (existing != null) {
			return existing;
		}

		EntityManager toReturn;
		EntityManager toClose;

		try {
			if (txContext.getTransactionStatus() == NO_TRANSACTION) {
				toClose = emf.createEntityManager();
				toReturn = new ScopedEntityManagerWrapper(toClose);
			} else if (txContext.supportsLocal()) {
				toClose = emf.createEntityManager();
				toReturn = new TxEntityManagerWrapper(toClose);
				txContext.registerLocalResource(getLocalResource(toClose));
				toClose.getTransaction().begin();
			} else {
				throw new TransactionException(
						"There is a transaction active, but it does not support local participants");
			}
		} catch (Exception sqle) {
			throw new TransactionException(
					"There was a problem getting hold of a database connection",
					sqle);
		}

		
		txContext.postCompletion(x -> {
				try {
					toClose.close();
				} catch (PersistenceException sqle) {
					// TODO log this
				}
			});
		
		txContext.putScopedValue(resourceId, toReturn);
		
		return toReturn;
	}

	
	private LocalResource getLocalResource(EntityManager em) {
		return new LocalResource() {
			@Override
			public void commit() throws TransactionException {
				try {
					em.getTransaction().commit();
				} catch (PersistenceException e) {
					throw new TransactionException(
							"An error occurred when committing the connection", e);
				}
			}

			@Override
			public void rollback() throws TransactionException {
				try {
					em.getTransaction().rollback();
				} catch (PersistenceException e) {
					throw new TransactionException(
							"An error occurred when rolling back the connection", e);
				}
			}

		};
	}
}
