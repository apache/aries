package org.apache.aries.tx.control.jpa.common.impl;

import javax.persistence.EntityManager;
import javax.persistence.TransactionRequiredException;

public class ScopedEntityManagerWrapper extends EntityManagerWrapper {

	private final EntityManager entityManager;
	
	public ScopedEntityManagerWrapper(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	protected EntityManager getRealEntityManager() {
		return entityManager;
	}

	@Override
	public void close() {
		// A no op
	}

	@Override
	public void joinTransaction() {
		throw new TransactionRequiredException("This EntityManager is being used in the No Transaction scope. There is no transaction to join.");
	}

	@Override
	public boolean isJoinedToTransaction() {
		return false;
	}
}
