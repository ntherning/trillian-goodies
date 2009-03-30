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

import java.util.Map;

/**
 * Interface implemented by all mocked bean instances returned by 
 * the methods in {@link MockBeanFactory}. Cast the returned
 * object to {@link MockBean} to access the methods in this 
 * interface.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public interface MockBean {
    /**
     * Returns the value of the property with the specified name.
     * Useful when you need to read a write-only property.
     * 
     * @param name the name of the property to get.
     * @return the value or <code>null</code> if not set.
     */
    Object _get(String name);
    
    /**
     * Sets the property with the specified name to the specified
     * value. Useful when you need to set a read-only property.
     * 
     * @param name the name of the property.
     * @param value the new value.
     * @return this {@link MockBean} object.
     */
    MockBean _set(String name, Object value);
    
    /**
     * Returns the backing {@link Map} containing the currently set 
     * properties.
     *  
     * @return the {@link Map} 
     */
    Map<String, Object> _props();
    
    /**
     * Returns a copy of this {@link MockBean}. NOTE: the target
     * object will not be cloned. The new {@link MockBean} will
     * use the same target object.
     * 
     * @return the copy.
     */
    MockBean _copy();
}
