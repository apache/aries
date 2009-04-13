package org.apache.felix.blueprint.context;

import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.xbean.recipe.Repository;
import org.apache.xbean.recipe.DefaultRepository;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.CollectionRecipe;
import org.apache.xbean.recipe.MapRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.MapValue;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 13, 2009
 * Time: 5:36:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Instanciator {

    public static Repository createRepository(ComponentDefinitionRegistry registry) throws Exception {
        Repository repository = new DefaultRepository();
        // Create recipes
        for (String name : (Set<String>) registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            if (component instanceof LocalComponentMetadata) {
                LocalComponentMetadata local = (LocalComponentMetadata) component;
                ObjectRecipe recipe = new ObjectRecipe(local.getClassName());
                recipe.setName(name);
                repository.add(name, recipe);
            } else {
                throw new IllegalStateException("Unsupported component " + component.getClass());
            }
        }
        // Populate recipes
        for (String name : (Set<String>) registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            if (component instanceof LocalComponentMetadata) {
                LocalComponentMetadata local = (LocalComponentMetadata) component;
                ObjectRecipe recipe = (ObjectRecipe) repository.get(local.getName());
                for (PropertyInjectionMetadata property : (Collection<PropertyInjectionMetadata>) local.getPropertyInjectionMetadata()) {
                    Object value = getValue(repository, property.getValue());
                    recipe.setProperty(property.getName(), value);
                }
                // TODO: constructor args
                // TODO: init-method
                // TODO: destroy-method
                // TODO: lazy
                // TODO: scope
                // TODO: factory-method
                // TODO: factory-component
            } else {
                // TODO
                throw new IllegalStateException("Unsupported component " + component.getClass());
            }
        }
        return repository;
    }

    private static Object getValue(Repository repository, Value v) {
        Object value;
        if (v instanceof NullValue) {
            value = null;
        } else if (v instanceof TypedStringValue) {
            value = ((TypedStringValue) v).getStringValue();
            // TODO: type name ?
        } else if (v instanceof ReferenceValue) {
            String componentName = ((ReferenceValue) v).getComponentName();
            if (repository.contains(componentName)) {
                value = repository.get(componentName);
            } else {
                throw new IllegalStateException("Undefined reference: " + componentName);
            }
        } else if (v instanceof ListValue) {
            CollectionRecipe cr = new CollectionRecipe(ArrayList.class);
            for (Value lv : (List<Value>) ((ListValue) v).getList()) {
                cr.add(getValue(repository, lv));
            }
            value = cr;
            // TODO: ListValue#getValueType()
        } else if (v instanceof SetValue) {
            CollectionRecipe cr = new CollectionRecipe(HashSet.class);
            for (Value lv : (Set<Value>) ((SetValue) v).getSet()) {
                cr.add(getValue(repository, lv));
            }
            value = cr;
            // TODO: SetValue#getValueType()
        } else if (v instanceof MapValue) {
            MapRecipe mr = new MapRecipe(HashMap.class);
            for (Map.Entry<Value,Value> entry : ((Map<Value,Value>) ((MapValue) v).getMap()).entrySet()) {
                Object key = getValue(repository, entry.getKey());
                Object val = getValue(repository, entry.getValue());
                mr.put(key, val);
            }
            value = mr;
            // TODO: MapValue#getKeyType()
            // TODO: MapValue#getValueType()
        } else {
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        }
        return value;
    }

}
