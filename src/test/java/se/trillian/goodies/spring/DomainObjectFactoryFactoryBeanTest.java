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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ListableBeanFactory;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * Tests {@link DomainObjectFactoryFactoryBean}.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class DomainObjectFactoryFactoryBeanTest extends RMockTestCase {
    DomainObjectFactoryFactoryBean factoryBean;
    
    @Override
    protected void setUp() throws Exception {
        factoryBean = new DomainObjectFactoryFactoryBean();
        factoryBean.setInterfaceClass(Interface.class);
        factoryBean.setImplementationClass(Implementation.class);
    }

    public void testCreateFactoryConstructor1() throws Exception {
        ListableBeanFactory beanFactory = (ListableBeanFactory) mock(ListableBeanFactory.class);
        beanFactory.getBeansOfType(A.class);
        Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("a", new A());
        modify().returnValue(beans);
        
        startVerification();
        
        factoryBean.afterPropertiesSet();
        factoryBean.setBeanFactory(beanFactory);
        Interface.Factory factory = (Interface.Factory) factoryBean.getObject();
        Implementation impl = (Implementation) factory.create("Hello world!!!");
        assertEquals("Hello world!!!", impl.s);
        assertThat(impl.dependency, is.instanceOf(A.class));
    }
    
    public void testCreateFactoryConstructor2() throws Exception {
        ListableBeanFactory beanFactory = (ListableBeanFactory) mock(ListableBeanFactory.class);
        beanFactory.getBeansOfType(B.class);
        Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("b", new B());
        modify().returnValue(beans);
        
        startVerification();
        
        factoryBean.afterPropertiesSet();
        factoryBean.setBeanFactory(beanFactory);
        Interface.Factory factory = (Interface.Factory) factoryBean.getObject();
        Implementation impl = (Implementation) factory.create(1024);
        assertEquals(1024, impl.x);
        assertThat(impl.dependency, is.instanceOf(B.class));
    }
    
    public void testCreateFactoryConstructor3() throws Exception {
        ListableBeanFactory beanFactory = (ListableBeanFactory) mock(ListableBeanFactory.class);
        beanFactory.getBeansOfType(C.class);
        Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("c", new C());
        modify().returnValue(beans);
        
        startVerification();
        
        factoryBean.afterPropertiesSet();
        factoryBean.setBeanFactory(beanFactory);
        Interface.Factory factory = (Interface.Factory) factoryBean.getObject();
        Implementation impl = (Implementation) factory.create(Math.PI);
        assertEquals(Math.PI, impl.u);
        assertThat(impl.dependency, is.instanceOf(C.class));
    }
    
    public void testObjectMethods() throws Exception {
        factoryBean.afterPropertiesSet();
        Interface.Factory factory = (Interface.Factory) factoryBean.getObject();
        factory.toString();
        factory.equals(null);
        factory.hashCode();
    }
    
    public void testEmptyFactory() throws Exception {
        factoryBean.setFactoryClass(EmptyFactory.class);
        try {
            factoryBean.afterPropertiesSet();
            fail("Empty factory. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testInvalidFactory1() throws Exception {
        factoryBean.setFactoryClass(InvalidFactory1.class);
        try {
            factoryBean.afterPropertiesSet();
            fail("Invalid return type. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testInvalidFactory2() throws Exception {
        factoryBean.setFactoryClass(InvalidFactory2.class);
        try {
            factoryBean.afterPropertiesSet();
            fail("Invalid factory method name. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testInvalidFactory3() throws Exception {
        factoryBean.setFactoryClass(InvalidFactory3.class);
        try {
            factoryBean.afterPropertiesSet();
            fail("No matching constructor. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testInvalidFactory4() throws Exception {
        factoryBean.setFactoryClass(InvalidFactory4.class);
        try {
            factoryBean.afterPropertiesSet();
            fail("Ambiguous constructor. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public static interface Interface {
        interface Factory {
            Interface create(String s);
            Interface create(int x);
            Interface create(double u);
        }
    }

    interface EmptyFactory {
    }
    interface InvalidFactory1 {
        void create();
    }
    interface InvalidFactory2 {
        Interface foo(int a, int y);
    }
    interface InvalidFactory3 {
        Interface create();
    }
    interface InvalidFactory4 {
        Interface create(Object o);
    }
    
    public static class Implementation implements Interface {
        public String s;
        public int x;
        public double u;
        public Dependency dependency;
        public Implementation(String s, A a) {
            this.s = s;
            this.dependency = a;
        }
        public Implementation(int x, B b) {
            this.x = x;
            this.dependency = b;
        }
        public Implementation(double u, C c) {
            this.u = u;
            this.dependency = c;
        }
    }
    
    public abstract static class Dependency {
    }
    
    public static class A extends Dependency {
    }
    public static class B extends Dependency {
    }
    public static class C extends Dependency {
    }
}
