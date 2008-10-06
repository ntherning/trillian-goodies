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
package se.trillian.goodies.jpa;

import org.springframework.orm.jpa.support.JpaDaoSupport;

/**
 * Abstract generic Dao for making request with JPA. This class needs to know
 * the type of the items retrieved by this dao - both the implemented interface
 * (<code>ItemType</code>) and the actual JPA class (by implementing the
 * abstract method {@link #getJpaClass()}).
 * 
 * @author Henric Müller
 * @version $Id$
 */
public abstract class GenericJpaDaoSupport<ItemType> extends JpaDaoSupport {

    /**
     * Subclasses needs to implement this to provide the actual JPA class which
     * should be returned in load()-functions.
     * 
     * @return the class that JPA instantiates when returning items from the
     *         database.
     */
    protected abstract Class<? extends ItemType> getJpaClass();

    public void delete(ItemType item) {
        getJpaTemplate().remove(item);
    }

    public void save(ItemType item) {
        getJpaTemplate().persist(item);
    }

    @SuppressWarnings("all")
    public ItemType load(Object id) {
        return (ItemType) getJpaTemplate().find(getJpaClass(), id);
    }

}
