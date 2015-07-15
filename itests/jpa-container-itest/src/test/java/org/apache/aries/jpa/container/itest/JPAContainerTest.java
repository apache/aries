package org.apache.aries.jpa.container.itest;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.junit.Test;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public abstract class JPAContainerTest extends AbstractJPAItest {

    @Test
    public void testCarEMFBuilder() throws Exception {
        EntityManagerFactoryBuilder emfBuilder = getService(EntityManagerFactoryBuilder.class,
                                                            "(osgi.unit.name=" + DSF_TEST_UNIT + ")");
        Map<String, Object> props = new HashMap<String, Object>();
        EntityManagerFactory emf = emfBuilder.createEntityManagerFactory(props);
        carLifecycleRL(emf.createEntityManager());
    }

    @Test
    public void testCarEMF() throws Exception {
        carLifecycleRL(getEMF(TEST_UNIT).createEntityManager());
    }
    
    @Test
    public void testEMFXA() throws Exception {
        EntityManager em = getEMF(XA_TEST_UNIT).createEntityManager();
        carLifecycleXA(ut, em);
        em.close();
    }

    @Test
    public void testDataSourceFactoryLifecycle() throws Exception {
        carLifecycleRL(getEMF(DSF_TEST_UNIT).createEntityManager());
    }

    @Test
    public void testDataSourceFactoryXALifecycle() throws Exception {
        EntityManager em = getEMF(DSF_XA_TEST_UNIT).createEntityManager();
        carLifecycleXA(ut, em);
        em.close();
    }
    

    @Test
    public void testEmSupplier() throws Exception {
        EmSupplier emSupplier = getService(EmSupplier.class, "(osgi.unit.name=xa-test-unit)");
        emSupplier.preCall();
        EntityManager em = emSupplier.get();
        carLifecycleXA(ut, em);

        Query countQuery = em.createQuery("SELECT Count(c) from Car c");
        assertEquals(0l, countQuery.getSingleResult());

        ut.begin();
        em.joinTransaction();
        em.persist(createBlueCar());
        em.persist(createGreenCar());
        ut.commit();

        assertEquals(2l, countQuery.getSingleResult());

        TypedQuery<Car> carQuery = em.createQuery("Select c from Car c ORDER by c.engineSize", Car.class);
        List<Car> list = carQuery.getResultList();
        assertEquals(2, list.size());

        assertBlueCar(list.get(0));
        assertGreenCar(list.get(1));

        ut.begin();
        em.joinTransaction();
        Car car = em.find(Car.class, BLUE_CAR_PLATE);
        car.setNumberOfSeats(2);
        car.setEngineSize(2000);
        car.setColour("red");
        em.remove(em.find(Car.class, GREEN_CAR_PLATE));
        em.persist(createBlackCar());
        ut.commit();

        assertEquals(2l, countQuery.getSingleResult());

        list = carQuery.getResultList();
        assertEquals(2, list.size());

        assertEquals(2, list.get(0).getNumberOfSeats());
        assertEquals(800, list.get(0).getEngineSize());
        assertEquals("black", list.get(0).getColour());
        assertEquals("C3CCC", list.get(0).getNumberPlate());

        assertEquals(2, list.get(1).getNumberOfSeats());
        assertEquals(2000, list.get(1).getEngineSize());
        assertEquals("red", list.get(1).getColour());
        assertEquals("A1AAA", list.get(1).getNumberPlate());
        
        ut.begin();
        em.joinTransaction();
        delete(em, "C3CCC");
        delete(em, "A1AAA");
        ut.commit();
        emSupplier.postCall();
    }

    private Car createBlackCar() {
        Car car;
        car = new Car();
        car.setNumberOfSeats(2);
        car.setEngineSize(800);
        car.setColour("black");
        car.setNumberPlate("C3CCC");
        return car;
    }
}
