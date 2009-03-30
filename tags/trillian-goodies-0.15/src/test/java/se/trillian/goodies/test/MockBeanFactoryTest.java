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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Tests {@link MockBeanFactory}.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class MockBeanFactoryTest extends TestCase {

    public void testDefaults() throws Exception {
        Fruit f = MockBeanFactory.stub(Fruit.class);
        assertEquals(false, f.isSweet());
        assertEquals(0.0, f.getWeight());
        assertEquals(null, f.getName());
    }
    
    public void testStub() throws Exception {
        Fruit f = MockBeanFactory.stub(Fruit.class, "sweet", true, "weight", 10.5, "name", "banana");
        assertEquals(true, f.isSweet());
        assertEquals(10.5, f.getWeight());
        assertEquals("banana", f.getName());

        f.setSweet(false);
        f.setWeight(2);
        f.setName("orange");
        assertEquals(false, f.isSweet());
        assertEquals(2.0, f.getWeight());
        assertEquals("orange", f.getName());
        
        ((MockBean) f)._set("sweet", true)._set("name", "apple")._set("weight", 5.5);
        assertEquals(true, f.isSweet());
        assertEquals(5.5, f.getWeight());
        assertEquals("apple", f.getName());
    }
    
    public void testMock() throws Exception {
        Fruit f = MockBeanFactory.mock(Fruit.class, new MockFruit(), "sweet", true, "weight", 10.5, "name", "banana");
        assertEquals(true, f.isSweet());
        assertEquals(10.5, f.getWeight());
        assertEquals("banana", f.getName());
        assertEquals(12345, f.compareTo(new MockFruit()));
    }
    
    public interface Fruit extends Comparable<Fruit> {
        boolean isSweet();
        void setSweet(boolean b);
        double getWeight();
        void setWeight(double weight);
        String getName();
        void setName(String name);
    }
    
    public static class MockFruit implements Fruit {

        public String getName() {
            throw new AssertionFailedError();
        }

        public double getWeight() {
            throw new AssertionFailedError();
        }

        public boolean isSweet() {
            throw new AssertionFailedError();
        }

        public void setName(String name) {
            throw new AssertionFailedError();
        }

        public void setSweet(boolean b) {
            throw new AssertionFailedError();
        }

        public void setWeight(double weight) {
            throw new AssertionFailedError();
        }

        public int compareTo(Fruit o) {
            assertNotNull(o);
            return 12345;
        }
        
    }
}
