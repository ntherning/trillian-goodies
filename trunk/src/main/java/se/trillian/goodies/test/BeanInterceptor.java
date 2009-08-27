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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * CGLIB {@link MethodInterceptor} which is used by {@link Dummy} to intercept
 * calls on dummy objects.
 *
 * @author Niklas Therning
 * @version $Id$
 */
class BeanInterceptor<T> implements MethodInterceptor {
    
    private final Class<T> clazz;
    private final Map<String, Object> properties;
    private final Object[] args;
    
    public static <T> T newInstance(Class<T> clazz, Object ... args) {
        try {
            BeanInterceptor<T> interceptor = new BeanInterceptor<T>(clazz, args);
            Enhancer e = new Enhancer();
            e.setSuperclass(clazz);
            e.setInterfaces(new Class[] {Bean.class});
            e.setCallback(interceptor);
            Class[] parameterTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i].getClass();
            }
            return (T) e.create(parameterTypes, args);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Error(e.getMessage());
        }
    }

    private BeanInterceptor(Class<T> clazz, Object[] args) {
        this.clazz = clazz;
        this.properties = new HashMap<String, Object>();
        this.args = args;
    }
    
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        
        if (args.length == 0) {
            if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && method.getReturnType() != Void.TYPE) {
                if (!Modifier.isAbstract(method.getModifiers())) {
                    return proxy.invokeSuper(obj, args);
                }
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
            if (method.getName().equals("_props")) {
                return properties;
            }
            if (method.getName().equals("_copy")) {
                return newInstance(clazz, this.args);
            }
        } else if (args.length == 2 && method.getName().equals("_set")) {
            Method setter = getSetterMethod(obj, (String) args[0]);
            if (setter != null) {
                return setter.invoke(obj, new Object[] {args[1]});
            }
            checkHasProperty(obj, (String) args[0]);
            properties.put((String) args[0], args[1]);
            return obj;
        } else if (args.length == 1 && method.getName().equals("_get")) {
            Method getter = getGetterMethod(obj, (String) args[0]);
            if (getter != null) {
                return getter.invoke(obj, new Object[0]);
            }
            checkHasProperty(obj, (String) args[0]);
            return properties.get(args[0]);
        } else if (args.length == 1 && method.getName().startsWith("set") && method.getReturnType() == Void.TYPE) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                return proxy.invokeSuper(obj, args);
            }
            properties.put(getPropertyName(method.getName()), args[0]);
            return null;
        }
        
        if (!Modifier.isAbstract(method.getModifiers())) {
            return proxy.invokeSuper(obj, args);
        }
        
        throw new UnsupportedOperationException(method.toGenericString());
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
        
    protected Method getGetterMethod(Object o, String propertyName) {
        String name = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        for (Method m : o.getClass().getMethods()) {
            if (m.getParameterTypes().length == 0 && (m.getName().equals("get" + name) || m.getName().equals("is" + name)) && m.getReturnType() != Void.TYPE) {
                return m;
            }
        }
        return null;
    }
    
    protected Method getSetterMethod(Object o, String propertyName) {
        String name = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        for (Method m : o.getClass().getMethods()) {
            if (m.getParameterTypes().length == 0 && m.getName().equals("set" + name) && m.getReturnType() == Void.TYPE) {
                return m;
            }
        }
        return null;
    }
    
    protected String getPropertyName(String name) {
        name = name.startsWith("is") ? name.substring(2) : name.substring(3);
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
}
