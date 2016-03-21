package org.apache.aries.blueprint.plugin.model;


public class ProducedBean extends Bean {
    public String factoryMethod;
    public BeanRef factoryBean;

    public ProducedBean(Class<?> clazz, BeanRef factoryBean, String factoryMethod) {
        super(clazz);
        this.factoryBean = factoryBean;
        this.factoryMethod = factoryMethod;
    }

    public ProducedBean(Class<?> clazz, String id, BeanRef factoryBean, String factoryMethod) {
        super(clazz);
        this.id = id;
        this.factoryBean = factoryBean;
        this.factoryMethod = factoryMethod;
    }

    public void setSingleton(){
        this.isPrototype = false;
    }
}
