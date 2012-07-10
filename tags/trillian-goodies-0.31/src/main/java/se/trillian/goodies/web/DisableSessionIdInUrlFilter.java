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
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * {@link Filter} implementation which prevents the session id from being
 * encoded in the URL. 
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class DisableSessionIdInUrlFilter implements Filter {
    private static final String SESSION_ID_REG_EXP = "(?i);jsessionid=[0-9a-z]+";
    private static final Pattern SESSION_ID_PATTERN = 
        Pattern.compile(SESSION_ID_REG_EXP);

    private Pattern disableForPathPattern = null;
    
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        /*Do not remove session id from API requests.*/
        String path = request.getServletPath();
        if (path == null || disableForPathPattern == null || (!disableForPathPattern.matcher(path).matches())) {
            request = new RequestWrapper(request);
            response = new ResponseWrapper(response);
        }
        chain.doFilter(request, response);
    }

    public void init(FilterConfig fc) throws ServletException {
        String pattern = fc.getInitParameter("disable-for-path-pattern");
        disableForPathPattern = Pattern.compile(pattern);
    }
    
    public void destroy() {}
    
    private static class RequestWrapper extends HttpServletRequestWrapper {
        private String requestUri = null;
        private String requestUrl = null;
        
        public RequestWrapper(HttpServletRequest request) {
            super(request);
        }
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }
        public StringBuffer getRequestURL() {
            if (requestUrl == null) {
                requestUrl = removeSessionId(super.getRequestURL().toString());
            }
            return new StringBuffer(requestUrl);
        }
        public String getRequestURI() {
            if (requestUri == null) {
                requestUri = removeSessionId(super.getRequestURI());
            }
            return requestUri;
        }
        private String removeSessionId(String s) {
            return SESSION_ID_PATTERN.matcher(s).replaceFirst("");
        }
    }
    
    private static class ResponseWrapper extends HttpServletResponseWrapper {

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
        }
        public String encodeRedirectUrl(String url) {
            return url;
        }
        public String encodeRedirectURL(String url) {
            return url;
        }
        public String encodeUrl(String url) {
            return url;
        }
        public String encodeURL(String url) {
            return url;
        }
    }
    
}
