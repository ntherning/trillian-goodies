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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests {@link HostNameBasedPropertyPlaceHolderConfigurer}.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class HostNameBasedPropertyPlaceHolderConfigurerTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testConfigurer() throws Exception {
        HostNameBasedPropertyPlaceHolderConfigurer configurer = 
            new HostNameBasedPropertyPlaceHolderConfigurer() {
            @Override
            protected String getHostName() {
                return "foobar-10";
            }
        };
        configurer.setLocation(new ClassPathResource("/se/trillian/goodies/spring/spring.properties"));
        List<String> hostNameFilters = new ArrayList<String>();
        hostNameFilters.add("nonmatchingfilter=>$1");
        hostNameFilters.add("([a-z]+)-\\d+=>$1");
        configurer.setHostNameFilters(hostNameFilters);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("context.xml", this.getClass());
        context.addBeanFactoryPostProcessor(configurer);
        context.refresh();
        Map<String, String> fruits = (Map<String, String>) context.getBean("fruits");
        
        assertEquals("boquila", fruits.get("fruit1"));
        assertEquals("currant", fruits.get("fruit2"));
        assertEquals("blueberry", fruits.get("fruit3"));
        assertEquals("raspberry", fruits.get("fruit4"));
        assertEquals("peach", fruits.get("fruit5"));
        assertEquals("pear", fruits.get("fruit6"));
        
        Map<String, String> hostname = (Map<String, String>) context.getBean("hostname");
        assertEquals("foobar-10", hostname.get("hostname1"));
        assertEquals("foobar-10", hostname.get("hostname2"));
    }
    
}
