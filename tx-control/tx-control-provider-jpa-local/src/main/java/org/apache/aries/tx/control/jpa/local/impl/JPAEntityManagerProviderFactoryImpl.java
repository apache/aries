package org.apache.aries.tx.control.jpa.local.impl;

import static java.util.Optional.ofNullable;
import static javax.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;

public class JPAEntityManagerProviderFactoryImpl implements JPAEntityManagerProviderFactory {

	@Override
	public JPAEntityManagerProvider getProviderFor(EntityManagerFactoryBuilder emfb, Map<String, Object> jpaProperties,
			Map<String, Object> resourceProviderProperties) {
		checkEnlistment(resourceProviderProperties);
		
		EntityManagerFactory emf = emfb.createEntityManagerFactory(jpaProperties);
		
		validateEMF(emf);
		
		return new JPAEntityManagerProviderImpl(emf);
	}

	private void validateEMF(EntityManagerFactory emf) {
		Object o = emf.getProperties().get("javax.persistence.transactionType");
		
		PersistenceUnitTransactionType tranType;
		if(o instanceof PersistenceUnitTransactionType) {
			tranType = (PersistenceUnitTransactionType) o;
		} else if (o instanceof String) {
			tranType = PersistenceUnitTransactionType.valueOf(o.toString());
		} else {
			//TODO log this?
			tranType = RESOURCE_LOCAL;
		}
		
		if(RESOURCE_LOCAL != tranType) {
			throw new IllegalArgumentException("The supplied EntityManagerFactory is not declared RESOURCE_LOCAL");
		}
	}

	@Override
	public JPAEntityManagerProvider getProviderFor(EntityManagerFactory emf, Map<String, Object> jpaProperties,
			Map<String, Object> resourceProviderProperties) {
		checkEnlistment(resourceProviderProperties);
		validateEMF(emf);
		
		return new JPAEntityManagerProviderImpl(emf);
	}

	private void checkEnlistment(Map<String, Object> resourceProviderProperties) {
		if (toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, false)) {
			throw new TransactionException("This Resource Provider does not support XA transactions");
		} else if (!toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true)) {
			throw new TransactionException(
					"This Resource Provider always enlists in local transactions as it does not support XA");
		}
	}
	
	private boolean toBoolean(Map<String, Object> props, String key, boolean defaultValue) {
		Object o =  ofNullable(props)
			.map(m -> m.get(key))
			.orElse(defaultValue);
		
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue();
		} else if(o instanceof String) {
			return Boolean.parseBoolean((String) o);
		} else {
			throw new IllegalArgumentException("The property " + key + " cannot be converted to a boolean");
		}
	}
}
