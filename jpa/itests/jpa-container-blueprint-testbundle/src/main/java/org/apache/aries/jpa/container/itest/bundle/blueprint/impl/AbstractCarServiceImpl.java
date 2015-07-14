package org.apache.aries.jpa.container.itest.bundle.blueprint.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.aries.jpa.container.itest.entities.CarService;

public abstract class AbstractCarServiceImpl implements CarService {
    @PersistenceContext(unitName = "test_unit_blueprint")
    protected EntityManager em;
}
