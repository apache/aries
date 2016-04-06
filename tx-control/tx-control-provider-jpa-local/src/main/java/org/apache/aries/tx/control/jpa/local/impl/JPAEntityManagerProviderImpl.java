package org.apache.aries.tx.control.jpa.local.impl;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;

public class JPAEntityManagerProviderImpl implements JPAEntityManagerProvider {

	private final UUID					uuid	= UUID.randomUUID();

	private final EntityManagerFactory 	emf;
	
	public JPAEntityManagerProviderImpl(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Override
	public EntityManager getResource(TransactionControl txControl) throws TransactionException {
		return new TxContextBindingEntityManager(txControl, emf, uuid);
	}
}
