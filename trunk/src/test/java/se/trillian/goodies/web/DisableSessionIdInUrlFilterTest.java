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
package se.trillian.goodies.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests {@link DisableSessionIdInUrlFilter}.
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class DisableSessionIdInUrlFilterTest extends TestCase {
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    DisableSessionIdInUrlFilter filter;
    
    protected void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse() {
            public String encodeURL(String url) {
                return url + ";jsessionid=2187WKEJH8923JWEKH";
            }
            public String encodeRedirectURL(String url) {
                return url + ";jsessionid=WQ87621JHED89WEQDD";
            }
        };
        filter = new DisableSessionIdInUrlFilter();
    }
    
    public void testSessionIdInRequestUrlIsRemoved() throws Exception {
        request.setServerName("www.spamdrain.net");
        request.setRequestURI("/home_en.html;jsessionid=1047kv5xa62lf");
        request.setRequestedSessionIdFromURL(true);
        final boolean[] called = new boolean[] {false};
        
        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest req, ServletResponse res)
                    throws IOException, ServletException {
                
                HttpServletRequest request = (HttpServletRequest) req;
                assertFalse(request.isRequestedSessionIdFromURL());
                assertEquals("/home_en.html", request.getRequestURI());
                assertEquals("http://www.spamdrain.net:80/home_en.html", 
                             request.getRequestURL().toString());
                called[0] = true;
            }
        });
        
        assertTrue(called[0]);
    }
    
    public void testSessionIdInEncodeUrlIsRemoved() throws Exception {
        final boolean[] called = new boolean[] {false};
        
        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest req, ServletResponse res)
                    throws IOException, ServletException {
                
                HttpServletResponse response = (HttpServletResponse) res;
                assertEquals("/home_en.html", response.encodeRedirectURL("/home_en.html"));
                assertEquals("/home_en.html", response.encodeURL("/home_en.html"));
                called[0] = true;
            }
        });
        
        assertTrue(called[0]);
    }
}
