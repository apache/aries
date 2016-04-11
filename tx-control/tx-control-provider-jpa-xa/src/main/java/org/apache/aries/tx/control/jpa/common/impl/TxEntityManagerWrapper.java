package org.apache.aries.tx.control.jpa.common.impl;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.osgi.service.transaction.control.TransactionException;

public class TxEntityManagerWrapper extends EntityManagerWrapper {

	private final EntityManager entityManager;
	
	public TxEntityManagerWrapper(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	protected EntityManager getRealEntityManager() {
		return entityManager;
	}

	@Override
	public void close() {
		// A no-op
	}

	@Override
	public void joinTransaction() {
		// A no-op
	}

	@Override
	public boolean isJoinedToTransaction() {
		return true;
	}

	@Override
	public EntityTransaction getTransaction() {
		throw new TransactionException("Programmatic transaction management is not supported for a Transactional EntityManager");
	}
}
