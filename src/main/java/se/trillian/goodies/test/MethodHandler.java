/*
 * Copyright (c) 2004-2009, Trillian AB. All Rights Reserved.
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

/**
 * Handler used by {@link MockBean}s to handle a method call which isn't a
 * property getter or setter.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public interface MethodHandler<T> {
    /**
     * Called by the {@link MockBean} when a method called on it isn't a getter
     * or setter for a property.
     * 
     * @param self the {@link MockBean} object the call was made on.
     * @param method the method which was invoked.
     * @param args the method arguments.
     * @return the value returned here will be returned by the {@link MockBean}.
     */
    Object call(T self, Method method, Object[] args);
}
