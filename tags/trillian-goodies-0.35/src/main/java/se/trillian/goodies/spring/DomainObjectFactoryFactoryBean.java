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
package se.trillian.goodies.spring;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * Spring {@link FactoryBean} which creates factories for domain objects. The 
 * <code>interfaceClass</code> property specifies the domain object interface. 
 * The <code>implementationClass</code> specifies the concrete domain object 
 * implementation class. The factory class to be implemented is specified 
 * using the <code>factoryClass</code> property. If <code>factoryClass</code>
 * isn't set the domain object interface must have an inner interface named 
 * <code>Factory</code> which will be used. 
 * <p>
 * All methods in the factory interface
 * must be named <code>create</code>, must return instances of 
 * <code>interfaceClass</code> and must have matching constructors in
 * <code>implementaionClass</code>. A factory method matches a constructor if
 * the parameter types of the factory method are a prefix of the constructor 
 * parameter types. The parameters which aren't specified by the factory method
 * will be injected from the Spring bean factory which created this 
 * {@link FactoryBean}.
 * </p>
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class DomainObjectFactoryFactoryBean extends AbstractFactoryBean implements BeanFactoryAware {
    
    private static final Object[] EMPTY_ARGS = new Object[0];
    
    private Class<?> interfaceClass;
    private Class<?> implementationClass;
    private Class<?> factoryClass;
    private ListableBeanFactory beanFactory;
    
    public void setImplementationClass(Class<?> implementationClass) {
        this.implementationClass = implementationClass;
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public void setFactoryClass(Class<?> factoryClass) {
        this.factoryClass = factoryClass;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
       this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    @Override
    protected Object createInstance() throws Exception {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(), 
                new Class[] {factoryClass}, new FactoryInvocationHandler());
    }

    public Class<?> getObjectType() {
        return factoryClass;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(interfaceClass, "interfaceClass");
        Assert.notNull(implementationClass, "implementationClass");
        if (factoryClass == null) {
            factoryClass = interfaceClass.getClassLoader()
                .loadClass(interfaceClass.getName() + "$Factory");
        }
        Assert.isTrue(factoryClass.isInterface(), "Factory class '" 
                + factoryClass + "' is not an interface");

        for (Method m: factoryClass.getMethods()) {
            Assert.isTrue("create".equals(m.getName()), "Domain object factory method '" 
                    + m + "' is invalid. All methods must be named 'create'.");
            Assert.isTrue(interfaceClass.equals(m.getReturnType()), "Factory method '" 
                    + m + "' does not return instances of '" + interfaceClass + "'");
            findMatchingConstructor(implementationClass, m);
        }
        Assert.isTrue(factoryClass.getMethods().length > 0, "Factory class '" 
                + factoryClass + "' does not define any methods");
        
        super.afterPropertiesSet();
    }

    private static Constructor<?> findMatchingConstructor(Class<?> clazz, Method m) {
        LinkedList<Constructor<?>> constructors = new LinkedList<Constructor<?>>();
        Constructor<?> directMatch = null;
        for (Constructor<?> c: clazz.getDeclaredConstructors()) {
            if (isParameterTypesPrefix(m.getParameterTypes(), c.getParameterTypes())) {
                constructors.add(c);
                if (directMatch == null && isParameterTypesPrefix(c.getParameterTypes(), m.getParameterTypes())) {
                    directMatch = c;
                }
            }
        }
        if (constructors.isEmpty()) {
            throw new IllegalArgumentException("No matching constructor found in "
                        + "implementation class '" + clazz + "' for factory method '" + m + "'");
        }
        if (constructors.size() > 1) {
            if (directMatch != null) {
                return directMatch;
            }
            throw new IllegalArgumentException("More than 1 matching constructor "
                        + "found in implementation class '" + clazz + "' for factory method '" + m + "'");
        }
        return constructors.getFirst();
    }
    
    private static boolean isParameterTypesPrefix(Class<?>[] prefixTypes, Class<?>[] types) {
        if (prefixTypes.length > types.length) {
            return false;
        }
        for (int i = 0; i < prefixTypes.length; i++) {
            if (!types[i].equals(prefixTypes[i])) {
                return false;
            }
        }
        return true;
    }
    
    private class FactoryInvocationHandler implements InvocationHandler {
        private final Map<Method, Constructor<?>> constructors;
        
        public FactoryInvocationHandler() {
            this.constructors = new HashMap<Method, Constructor<?>>();
            
            for (Method m: factoryClass.getMethods()) {
                this.constructors.put(m, findMatchingConstructor(implementationClass, m));
            }
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class.equals(method.getDeclaringClass())) {
                if (method.getName().equals("toString")) {
                    return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
                }
                if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(this);
                }
                if (method.getName().equals("equals")) {
                    return args[0] == this;
                }
            }
            if (args == null) {
                args = EMPTY_ARGS;
            }
            Constructor<?> constructor = constructors.get(method);
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == args.length) {
                return constructor.newInstance(args);
            }
            
            Object[] modifiedArgs = new Object[paramTypes.length];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            
            for (int i = args.length; i < modifiedArgs.length; i++) {
                modifiedArgs[i] = BeanFactoryUtils.beanOfType(beanFactory, paramTypes[i]);
            }
            
            return constructor.newInstance(modifiedArgs);
        }
        
    }
}
