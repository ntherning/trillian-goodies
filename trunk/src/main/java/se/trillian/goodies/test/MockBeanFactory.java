/*
 * Copyright (c) 2004-2008, Trillian AB. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trillian.goodies.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Factory for creating simple beans implementing interfaces
 * for domain objects. The returned instances will 
 * implement the {@link MockBean} interface.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class MockBeanFactory {

    /**
     * Creates a stub implementing the specified interface {@link Class} and
     * initializes the properties according to the specified array.
     * 
     * @param clazz the interface {@link Class} to create a stub for.
     * @param props pairs of property names and values. Must contain an even 
     *        number of objects.
     * @return the stub instance.
     */
    public static <T> T stub(Class<T> clazz, Object ... props) {
        return mock(clazz, null, props);
    }
    
    /**
     * Creates a stub implementing the specified interface {@link Class} and
     * initializes the properties according to the specified {@link Map}.
     * 
     * @param clazz the interface {@link Class} to create a stub for.
     * @param properties a {@link Map} used to initialize the properties.
     * @return the stub instance.
     */
    public static <T> T stub(Class<T> clazz, Map<String, Object> properties) {
        return mock(clazz, null, properties);
    }

    /**
     * Creates a mock implementing the specified interface {@link Class} and
     * initializes the properties according to the specified {@link Map}.
     * Method calls on the returned instance which don't match a property
     * getter or setter will be forwarded to the target object.
     * 
     * @param clazz the interface {@link Class} to create a mock for.
     * @param target the target object.
     * @param properties a {@link Map} used to initialize the properties.
     * @return the mock instance.
     */
    public static <T> T mock(Class<T> clazz, T target, Map<String, Object> properties) {
        T bean = mock(clazz, target);
        for (Entry<String, Object> entry: properties.entrySet()) {
            ((MockBean) bean)._set(entry.getKey(), entry.getValue());
        }
        return bean;
    }
    
    /**
     * Creates a mock implementing the specified interface {@link Class} and
     * initializes the properties according to the specified {@link Map}.
     * Method calls on the returned instance which don't match a property
     * getter or setter will be forwarded to the target object.
     * 
     * @param clazz the interface {@link Class} to create a mock for.
     * @param target the target object.
     * @param props pairs of property names and values. Must contain an even 
     *        number of objects.
     * @return the mock instance.
     */
    public static <T> T mock(Class<T> clazz, T target, Object ... props) {
        if (props.length % 2 == 1) {
            throw new IllegalArgumentException();
        }
        @SuppressWarnings("unchecked")
        T bean = (T) Proxy.newProxyInstance(clazz.getClassLoader(), 
                                      new Class[] {clazz, MockBean.class}, 
                new MockBeanInvocationHandler(clazz, target));
        for (int i = 0; i < props.length; i += 2) {
            ((MockBean) bean)._set(props[i].toString(), props[i + 1]);
        }
        return bean;
    }
    
    private static class MockBeanInvocationHandler<T> implements InvocationHandler {
        private final Class<T> clazz;
        private final Map<String, Object> properties;
        private final T target;
        
        public MockBeanInvocationHandler(Class<T> clazz, T target) {
            this.clazz = clazz;
            this.properties = new HashMap<String, Object>();
            this.target = target;
        }
        
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            
            if (args == null) {
                if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                    String propName = getPropertyName(method.getName());
                    if (properties.containsKey(propName)) {
                        return properties.get(propName);
                    }
                    if (method.getReturnType().isPrimitive()) {
                        if (Boolean.TYPE.equals(method.getReturnType())) {
                            return Boolean.FALSE;
                        }
                        if (Float.TYPE.equals(method.getReturnType())) {
                            return Float.valueOf(0.0f);
                        }
                        if (Double.TYPE.equals(method.getReturnType())) {
                            return Double.valueOf(0.0);
                        }
                        if (Byte.TYPE.equals(method.getReturnType())) {
                            return Byte.valueOf((byte) 0);
                        }
                        if (Short.TYPE.equals(method.getReturnType())) {
                            return Short.valueOf((short) 0);
                        }
                        if (Integer.TYPE.equals(method.getReturnType())) {
                            return Integer.valueOf(0);
                        }
                        if (Long.TYPE.equals(method.getReturnType())) {
                            return Long.valueOf(0L);
                        }
                        if (Character.TYPE.equals(method.getReturnType())) {
                            return Character.valueOf((char) 0);
                        }
                    }
                    return null;
                }
                if (method.getName().equals("hashCode")) {
                    return properties.hashCode();
                }
                if (method.getName().equals("toString")) {
                    return properties.toString();
                }
                if (method.getName().equals("_props")) {
                    return properties;
                }
                if (method.getName().equals("_copy")) {
                    return mock(clazz, target, new HashMap<String, Object>(properties));
                }
            }
            if (args != null) {
                if (args.length == 2 && method.getName().equals("_set")) {
                    checkHasProperty(proxy, (String) args[0]);
                    properties.put((String) args[0], args[1]);
                    return proxy;
                }
                if (args.length == 1 && method.getName().equals("_get")) {
                    return properties.get(args[0]);
                }
                if (args.length == 1 && method.getName().startsWith("set")) {
                    properties.put(getPropertyName(method.getName()), args[0]);
                    return null;
                }
                if (args.length == 1 && method.getName().equals("equals")) {
                    if (!(args[0] instanceof MockBean)) {
                        return false;
                    }
                    MockBean that = (MockBean) args[0];
                    return clazz.isInstance(that) && properties.equals(that._props());
                }
            }
            
            if (target != null) {
                return method.invoke(target, args);
            }
            return null;
        }
        
        protected void checkHasProperty(Object o, String propertyName) {
            String name = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            for (Method m : o.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && m.getName().equals("get" + name)) {
                    return;
                }
                if (m.getParameterTypes().length == 0 && m.getName().equals("is" + name)) {
                    return;
                }
                if (m.getParameterTypes().length == 1 && m.getName().equals("set" + name)) {
                    return;
                }
            }
            throw new RuntimeException("Unknown property: " + propertyName);
        }
        
        protected String getPropertyName(String name) {
            name = name.startsWith("is") ? name.substring(2) : name.substring(3);
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        
    }
    
}
