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
package se.trillian.goodies.spring.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.springframework.orm.jpa.JpaCallback;
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
public abstract class GenericJpaDaoSupport<ItemType, IdType> extends JpaDaoSupport {

    public static final String DEFAULT_ID_PROPERTY_NAME = "id";
    
    private String deleteQuery = null;
    
    /**
     * Subclasses needs to implement this to provide the actual JPA class which
     * should be returned in load()-functions.
     * 
     * @return the class that JPA instantiates when returning items from the
     *         database.
     */
    protected abstract Class<? extends ItemType> getJpaClass();

    protected String getIdPropertyName() {
        return DEFAULT_ID_PROPERTY_NAME;
    }
    
    protected String getEntityName() {
        return getJpaClass().getSimpleName();
    }
    
    public void delete(ItemType item) {
        getJpaTemplate().remove(item);
    }

    
    public void deleteById(final IdType id) {
        if (deleteQuery == null) {
            deleteQuery = "delete from " + getEntityName() + " where " + getIdPropertyName() + "=?";
        }
        getJpaTemplate().execute(new JpaCallback() {
            public Object doInJpa(EntityManager entityManager) throws PersistenceException {
                return entityManager.createQuery(deleteQuery).setParameter(0, id).executeUpdate();
            }
        });
    }

    public void save(ItemType item) {
        getJpaTemplate().persist(item);
    }

    @SuppressWarnings("all")
    public ItemType load(IdType id) {
        return (ItemType) getJpaTemplate().find(getJpaClass(), id);
    }

}
