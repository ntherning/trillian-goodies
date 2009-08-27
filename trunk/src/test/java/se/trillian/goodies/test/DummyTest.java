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

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Tests {@link Dummy}.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class DummyTest extends TestCase {

    interface User {
        String getFirstName();
        String getLastName();
        String getDisplayName();
        List<String> getAddressLines();
        Map<String, Object> getSettings();
        int doSomeWork(String value);
    }
    
    public void testSetSingle() throws Exception {
        abstract class DUser implements User, Bean<DUser> {}
        
        DUser user = new Dummy<DUser>(this) {{
            set("firstName", "Bart");
            set("lastName", "Simpson");
        }}.getObject();
        
        assertEquals("Bart", user._get("firstName"));
        assertEquals("Bart", user.getFirstName());
        assertEquals("Simpson", user._get("lastName"));
        assertEquals("Simpson", user.getLastName());
        
        user._set("firstName", "Lisa");
        assertEquals("Lisa", user._get("firstName"));
        assertEquals("Lisa", user.getFirstName());
    }
    
    public void testSetMultiple() throws Exception {
        abstract class DUser implements User, Bean<DUser> {}
        
        DUser user = new Dummy<DUser>(this) {{
            set(p("firstName", "Bart"), p("lastName", "Simpson"));
        }}.getObject();
        
        assertEquals("Bart", user._get("firstName"));
        assertEquals("Bart", user.getFirstName());
        assertEquals("Simpson", user._get("lastName"));
        assertEquals("Simpson", user.getLastName());
    }
    
    public void testSetList() throws Exception {
        abstract class DUser implements User, Bean<DUser> {}
        
        DUser user = new Dummy<DUser>(this) {{
            set("addressLines", list("512 High Street", "London", "WE4E 8GH"));
        }}.getObject();
        
        List<String> lines = user.getAddressLines();
        assertEquals(3, lines.size());
        assertEquals("512 High Street", lines.get(0));
        assertEquals("London", lines.get(1));
        assertEquals("WE4E 8GH", lines.get(2));
    }
    
    public void testSetMap() throws Exception {
        abstract class DUser implements User, Bean<DUser> {}
        
        DUser user = new Dummy<DUser>(this) {{
            set("settings", map(entry("setting1", true), entry("setting2", 1024)));
        }}.getObject();
        
        Map<String, Object> settings = user.getSettings();
        assertEquals(2, settings.size());
        assertEquals(true, settings.get("setting1"));
        assertEquals(1024, settings.get("setting2"));
    }
}
