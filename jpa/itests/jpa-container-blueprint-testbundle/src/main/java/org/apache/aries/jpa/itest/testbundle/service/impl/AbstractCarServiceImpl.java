package org.apache.aries.jpa.itest.testbundle.service.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.aries.jpa.itest.testbundle.service.CarService;

public abstract class AbstractCarServiceImpl implements CarService {
    @PersistenceContext(unitName = "test_unit_blueprint")
    protected EntityManager em;
}
