package org.apache.aries.jpa.support.impl;

import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.aries.jpa.impl.DummyCoordinator;
import org.apache.aries.jpa.template.EmConsumer;
import org.apache.aries.jpa.template.TransactionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class XAJpaTemplateTest
{
    private static EntityManagerFactory mockEmf() {
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        EntityManager em = mock(EntityManager.class);
        when(emf.createEntityManager()).thenReturn(em);
        return emf;
    }

    private EntityManagerFactory emf;
    private DummyCoordinator coordinator;
    private EMSupplierImpl emSupplier;

    @Before
    public void setup() throws IllegalStateException, SecurityException, HeuristicMixedException,
            HeuristicRollbackException, RollbackException, SystemException {
        this.emf = mockEmf();
        this.coordinator = new DummyCoordinator();
        this.emSupplier = new EMSupplierImpl("myunit", emf, coordinator);

    }
    
    @After
    public void cleanup() {
        this.emSupplier.close();
    }

    private TransactionManager mockTm() {
        TransactionManager tm = mock(TransactionManager.class);
        return tm;
    }

    @Test
    public void test_RollbackExceptionHandling_rollbackiscalledonmarkedrollback() throws Exception {
        TransactionManager tm = mockTm();
        when(tm.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION,
                Status.STATUS_MARKED_ROLLBACK);
        XAJpaTemplate tx = new XAJpaTemplate(emSupplier, tm, coordinator);
        tx.tx(TransactionType.Required, new EmConsumer() {
            public void accept(EntityManager em) {
                em.persist(new Object());
            }
        });
        verify(tm, times(3)).getStatus();
        verify(tm, never()).commit();
        verify(tm, times(1)).rollback();
    }

    @Test
    public void test_RollbackExceptionHandling_rollbackafterthrown()
            throws Exception {
        TransactionManager tm = mockTm();
        when(tm.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK);
        doThrow(new RollbackException().initCause(new OptimisticLockException())).when(tm).commit();
        XAJpaTemplate tx = new XAJpaTemplate(emSupplier, tm, coordinator);
        try {
            tx.tx(TransactionType.Required, new EmConsumer() {
                public void accept(EntityManager em) {
                    em.persist(new Object());
                }
            });
        } catch (RuntimeException e) {
            // this is ok
        }
        verify(tm, times(5)).getStatus();
        verify(tm, times(1)).commit();
        verify(tm, times(1)).rollback();
    }

}
