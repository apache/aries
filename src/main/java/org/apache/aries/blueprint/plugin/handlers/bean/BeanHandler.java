package org.apache.aries.blueprint.plugin.handlers.bean;

import org.apache.aries.blueprint.annotation.bean.Activation;
import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.bean.Scope;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.BeanFinder;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.FactoryMethodFinder;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;

import java.lang.reflect.AnnotatedElement;

public class BeanHandler implements
        BeanFinder<Bean>,
        FactoryMethodFinder<Bean>,
        NamedLikeHandler<Bean>,
        BeanAnnotationHandler<Bean> {
    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Class<Bean> getAnnotation() {
        return Bean.class;
    }

    @Override
    public String getName(Class clazz, AnnotatedElement annotatedElement) {
        Bean bean = annotatedElement.getAnnotation(Bean.class);
        if ("".equals(bean.id())) {
            return null;
        }
        return bean.id();
    }

    @Override
    public String getName(Object annotation) {
        Bean bean = Bean.class.cast(annotation);
        if ("".equals(bean.id())) {
            return null;
        }
        return bean.id();
    }

    @Override
    public void handleBeanAnnotation(AnnotatedElement annotatedElement, String id,
                                     ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        Bean annotation = annotatedElement.getAnnotation(Bean.class);
        if (annotation.activation() != Activation.DEFAULT) {
            beanEnricher.addAttribute("activation", annotation.activation().name().toLowerCase());
        }
        beanEnricher.addAttribute("scope", annotation.scope() == Scope.SINGLETON ? "singleton" : "prototype");
        if (annotation.dependsOn().length > 0) {
            StringBuilder dependsOn = new StringBuilder();
            for (int i = 0; i < annotation.dependsOn().length; i++) {
                if (i > 0) {
                    dependsOn.append(" ");
                }
                dependsOn.append(annotation.dependsOn()[i]);
            }
            beanEnricher.addAttribute("depends-on", dependsOn.toString());
        }
        if (!annotation.initMethod().isEmpty()) {
            beanEnricher.addAttribute("init-method", annotation.initMethod());
        }
        if (!annotation.destroyMethod().isEmpty()) {
            beanEnricher.addAttribute("destroy-method", annotation.destroyMethod());
        }
    }
}
