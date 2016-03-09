/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.container;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * XXXX: Currently, in case of arrays getActualTypeArgument(0) returns something similar to what
 * Class.getComponentType() does for arrays.  I don't think this is quite right since getActualTypeArgument()
 * should return the given parameterized type not the component type. Need to check this behavior with the spec.
 */
public class GenericType extends ReifiedType {

	private static final GenericType[] EMPTY = new GenericType[0];

    private static final Map<String, Class> primitiveClasses = new HashMap<String, Class>();

    static {
        primitiveClasses.put("int", int.class);
        primitiveClasses.put("short", short.class);
        primitiveClasses.put("long", long.class);
        primitiveClasses.put("byte", byte.class);
        primitiveClasses.put("char", char.class);
        primitiveClasses.put("float", float.class);
        primitiveClasses.put("double", double.class);
        primitiveClasses.put("boolean", boolean.class);
    }

    enum BoundType {
        Exact,
        Extends,
        Super
    }

    private GenericType[] parameters;
    private BoundType boundType;

	public GenericType(Type type) {
		this(getConcreteClass(type), boundType(type), parametersOf(type));
	}

    public GenericType(Class clazz, GenericType... parameters) {
        this(clazz, BoundType.Exact, parameters);
    }

    public GenericType(Class clazz, BoundType boundType, GenericType... parameters) {
        super(clazz);
        this.parameters = parameters;
        this.boundType = boundType;
    }

    public static GenericType parse(String rawType, final Object loader) throws ClassNotFoundException, IllegalArgumentException {
        final String type = rawType.trim();
        // Check if this is an array
        if (type.endsWith("[]")) {
            GenericType t = parse(type.substring(0, type.length() - 2), loader);
            return new GenericType(Array.newInstance(t.getRawClass(), 0).getClass(), t);
        }
        // Check if this is a generic
        int genericIndex = type.indexOf('<');
        if (genericIndex > 0) {
            if (!type.endsWith(">")) {
                throw new IllegalArgumentException("Can not load type: " + type);
            }
            GenericType base = parse(type.substring(0, genericIndex), loader);
            String[] params = type.substring(genericIndex + 1, type.length() - 1).split(",");
            GenericType[] types = new GenericType[params.length];
            for (int i = 0; i < params.length; i++) {
                types[i] = parse(params[i], loader);
            }
            return new GenericType(base.getRawClass(), types);
        }
        // Primitive
        if (primitiveClasses.containsKey(type)) {
            return new GenericType(primitiveClasses.get(type));
        }
        // Extends
        if (type.startsWith("? extends ")) {
            String raw = type.substring("? extends ".length());
            return new GenericType(((ClassLoader) loader).loadClass(raw), BoundType.Extends);
        }
        // Super
        if (type.startsWith("? super ")) {
            String raw = type.substring("? extends ".length());
            return new GenericType(((ClassLoader) loader).loadClass(raw), BoundType.Super);
        }
        // Class
        if (loader instanceof ClassLoader) {
            return new GenericType(((ClassLoader) loader).loadClass(type));
        } else if (loader instanceof Bundle) {
            try {
              return AccessController.doPrivileged(new PrivilegedExceptionAction<GenericType>() {
                public GenericType run() throws ClassNotFoundException {
                  return new GenericType(((Bundle) loader).loadClass(type));
                }
              });
            } catch (PrivilegedActionException pae) {
              Exception e = pae.getException();
              if (e instanceof ClassNotFoundException) 
                throw (ClassNotFoundException) e;
              else
                throw (RuntimeException) e;
            }
        } else if (loader instanceof ExecutionContext) {
            return new GenericType(((ExecutionContext) loader).loadClass(type));
        } else if (loader instanceof ExtendedBlueprintContainer) {
            return new GenericType(((ExtendedBlueprintContainer) loader).loadClass(type));
        } else {
            throw new IllegalArgumentException("Unsupported loader: " + loader);
        }
    }

    @Override
    public ReifiedType getActualTypeArgument(int i) {
        if (parameters.length == 0) {
            return super.getActualTypeArgument(i);
        }
        return parameters[i];
    }

    @Override
    public int size() {
        return parameters.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (boundType == BoundType.Extends) {
            sb.append("? extends ");
        } else if (boundType == BoundType.Super) {
            sb.append("? super ");
        }
        Class cl = getRawClass();
        if (cl.isArray()) {
            if (parameters.length > 0) {
                return parameters[0].toString() + "[]";
            } else {
                return cl.getComponentType().getName() + "[]";
            }
        }
        sb.append(cl.getName());
        if (parameters.length > 0) {
            sb.append("<");
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(parameters[i].toString());
            }
            sb.append(">");   
        }
        return sb.toString();
    }

    public boolean equals(Object object) {
        if (!(object instanceof GenericType)) {
            return false;
        }
        GenericType other = (GenericType) object;
        if (getRawClass() != other.getRawClass()) {
            return false;
        }
        if (boundType != other.boundType) {
            return false;
        }
        if (parameters == null) {
            return (other.parameters == null);
        } else {
            if (other.parameters == null) {
                return false;
            }
            if (parameters.length != other.parameters.length) {
                return false;
            }
            for (int i = 0; i < parameters.length; i++) {
                if (!parameters[i].equals(other.parameters[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    static ReifiedType bound(ReifiedType type) {
        if (type instanceof GenericType
                && ((GenericType) type).boundType != BoundType.Exact) {
            GenericType t = (GenericType) type;
            return new GenericType(t.getRawClass(), BoundType.Exact, t.parameters);
        }
        return type;
    }

    static BoundType boundType(ReifiedType type) {
        if (type instanceof GenericType) {
            return ((GenericType) type).boundType;
        } else {
            return BoundType.Exact;
        }
    }

    static BoundType boundType(Type type) {
        if (type instanceof WildcardType) {
            WildcardType wct = (WildcardType) type;
            return wct.getLowerBounds().length == 0
                    ? BoundType.Extends : BoundType.Super;
        }
        return BoundType.Exact;
    }

    static GenericType[] parametersOf(Type type) {
		if (type instanceof Class) {
		    Class clazz = (Class) type;
		    if (clazz.isArray()) {
                GenericType t = new GenericType(clazz.getComponentType());
                if (t.size() > 0) {
		            return new GenericType[] { t };
                } else {
                    return EMPTY;
                }
		    } else {
		        return EMPTY;
		    }
		}
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type [] parameters = pt.getActualTypeArguments();
            GenericType[] gts = new GenericType[parameters.length];
            for ( int i =0; i<gts.length; i++) {
                gts[i] = new GenericType(parameters[i]);
            }
            return gts;
        }
        if (type instanceof GenericArrayType) {
            return new GenericType[] { new GenericType(((GenericArrayType) type).getGenericComponentType()) };
        }
        if (type instanceof WildcardType) {
            return EMPTY;
        }
        if (type instanceof TypeVariable) {
            return EMPTY;
        }
        throw new IllegalStateException();
	}

	static Class<?> getConcreteClass(Type type) {
		Type ntype = collapse(type);
		if ( ntype instanceof Class )
			return (Class<?>) ntype;

		if ( ntype instanceof ParameterizedType )
			return getConcreteClass(collapse(((ParameterizedType)ntype).getRawType()));

		throw new RuntimeException("Unknown type " + type );
	}

	static Type collapse(Type target) {
		if (target instanceof Class || target instanceof ParameterizedType ) {
			return target;
		} else if (target instanceof TypeVariable) {
			return collapse(((TypeVariable<?>) target).getBounds()[0]);
		} else if (target instanceof GenericArrayType) {
			Type t = collapse(((GenericArrayType) target)
					.getGenericComponentType());
			while ( t instanceof ParameterizedType )
				t = collapse(((ParameterizedType)t).getRawType());
			return Array.newInstance((Class<?>)t, 0).getClass();
		} else if (target instanceof WildcardType) {
			WildcardType wct = (WildcardType) target;
			if (wct.getLowerBounds().length == 0)
				return collapse(wct.getUpperBounds()[0]);
			else
				return collapse(wct.getLowerBounds()[0]);
		}
		throw new RuntimeException("Huh? " + target);
	}

}
