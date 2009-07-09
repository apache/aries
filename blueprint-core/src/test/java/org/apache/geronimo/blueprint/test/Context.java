package org.apache.geronimo.blueprint.test;

import org.w3c.dom.Element;

import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;

public class Context {

    interface Builder<M extends Metadata, B extends Builder<M, B>> {
        M metadata();
    }

    interface ComponentBuilder<M extends ComponentMetadata, B extends ComponentBuilder<M, B>> extends Builder<M, B> {
        B id(String id);
        B activation(int activation);
        B dependsOn(String dependsOn);
        B removeDependsOn(String dependsOn);
        B clearDependsOn();
    }

    interface BeanBuilder extends ComponentBuilder<BeanMetadata, BeanBuilder> {
        BeanBuilder className(String className);
        BeanBuilder initMethod(String initMethod);
        BeanBuilder destroyMethod(String destroyMethod);
        BeanBuilder argument(BeanArgument argument);
        BeanBuilder argument(Metadata value);
        BeanBuilder argument(Metadata value, String valueType);
        BeanBuilder argument(Metadata value, int index);
        BeanBuilder argument(Metadata value, String valueType, int index);
        BeanBuilder removeArgument(BeanArgument argument);
        BeanBuilder clearArguments();
        BeanBuilder property(BeanProperty property);
        BeanBuilder property(String name, Metadata value);
        BeanBuilder removeProperty(BeanProperty property);
        BeanBuilder clearProperties();
        BeanBuilder factoryMethod(String factoryMethod);
        BeanBuilder factoryComponent(Target factoryComponent);
        BeanBuilder scope(String scope);
    }

    interface ServiceReferenceBuilder<M extends ServiceReferenceMetadata, B extends ServiceReferenceBuilder<M,B>> extends ComponentBuilder<M,B> {
        B availability(int availability);
        B interfaceName(String interfaceName);
        B componentName(String componentName);
        B filter(String filter);
        B listener(ReferenceListener listener);
        B listener(Target listenerComponent, String bindMethod, String unbindMethod);
        B removeListener(ReferenceListener listener);
        B clearListeners();
    }

    interface ReferenceBuilder extends ServiceReferenceBuilder<ReferenceMetadata, ReferenceBuilder> {
        long timeout(long timeout);
    }

    interface ReferenceListBuilder extends ServiceReferenceBuilder<ReferenceListMetadata, ReferenceListBuilder> {
        ReferenceListBuilder memberType(int memberType);
    }

    interface CollectionBuilder extends Builder<CollectionMetadata, CollectionBuilder> {
        CollectionBuilder collectionClass(Class<?> collectionClass);
        CollectionBuilder valueType(String valueType);
        CollectionBuilder value(Metadata value);
        CollectionBuilder removeValue(Metadata value);
        CollectionBuilder clearValues();
    }

    interface ServiceBuilder extends ComponentBuilder<ServiceMetadata, ServiceBuilder> {
        ServiceBuilder serviceComponent(Target target);
        // TODO
    }

    interface ParserContext {
        <M extends Metadata> M parse(ParserContext context, Element node, Class<M> type);
        <M extends Metadata> M metadata(Class<M> metadata);
        <M extends Metadata, B extends Builder<M, B>> B builder(Class<B> type, M metadata);
    }

    void test() {
        ParserContext ctx = getParserContext();

        BeanBuilder beanBuilder = ctx.builder(BeanBuilder.class, ctx.metadata(BeanMetadata.class));


        beanBuilder.id("id").activation(ComponentMetadata.ACTIVATION_EAGER).className("className");

        BeanMetadata bean = ctx.metadata(BeanMetadata.class);
        beanBuilder = ctx.builder(BeanBuilder.class, bean);
        beanBuilder.dependsOn("dependOn");

        ServiceBuilder serviceBuilder = ctx.builder(ServiceBuilder.class, ctx.metadata(ServiceMetadata.class));
        serviceBuilder.id("id");
    }

    public <T extends Metadata> T parse(ParserContext context, Element node, Class<T> type) {
        assert ComponentMetadata.class.isAssignableFrom(type);
        BeanBuilder beanBuilder = context.builder(BeanBuilder.class, context.metadata(BeanMetadata.class));
        beanBuilder.className("foo.bar.TraceBean");
        Element innerBeanElement = getInnerBeanElement(node);
        BeanMetadata innerBean = context.parse(context, innerBeanElement, BeanMetadata.class);
        beanBuilder.argument(innerBean);
        return type.cast(beanBuilder.metadata());
    }

    ParserContext getParserContext() {
        return null;
    }

    Element getInnerBeanElement(Element node) {
        return null;
    }

}
