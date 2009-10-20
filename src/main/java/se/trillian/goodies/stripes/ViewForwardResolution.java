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
package se.trillian.goodies.stripes;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;


/**
 * Stripes {@link Resolution} which forwards to a view. If the no args constructor
 * is used the view name will be derived from the URL of the current request.
 * 
 * <p>
 * Based on the view name, a JSP file will resolved according to 
 * <code>
 *   getPrefix() + viewName + getSuffix()
 * </code>
 * The default prefix is <code>/WEB-INF/jsp</code> and the default suffix is
 * <code>.jsp</code>.
 * </p>
 * <p>If no view name is specified, the view will be resolved using 
 * the protected method {@link #extractViewName(String, HttpServletRequest)}.
 * The default behaviour is to extract the view name from the request uri by first
 * removing the context path ({@link HttpServletRequest#getContextPath()} and then
 * apply the view name suffix pattern that can be set using 
 * {@link #setViewNameSuffixPattern(String)}. The default pattern is
 * <code>(_[\\w]+)?\\.[\\w]+$</code>. 
 * </p>
 * <p>
 * Examples of the default behaviour:
 * <ul>
 *   <li>http://www.host.com/theapp/subfolder/index.html -&gt; /subfolder/index -&gt; /WEB-INF/jsp/subfolder/index.jsp</li>
 *   <li>http://www.host.com/theapp/subfolder/abc_en.html -&gt; /subfolder/abc -&gt; /WEB-INF/jsp/subfolder/abc.jsp</li>
 *   <li>http://www.host.com/theapp/folder/subfolder/DoSomething.action -&gt; /folder/subfolder/DoSomething -&gt; /WEB-INF/jsp/folder/subfolder/DoSomething.jsp</li>
 * </ul>
 * </p>
 *
 * @author Henric Müller
 * @author NiklasTherning
 * @version $Id$
 */
public class ViewForwardResolution extends ForwardResolution {
    private static final Logger log = LoggerFactory.getLogger(ViewForwardResolution.class);
    
    private String prefix = "/WEB-INF/jsp";
    private String suffix = ".jsp";
    private String viewNameSuffixPattern = "(_[\\w]+)?\\.[\\w]+$";
    
    private boolean checkExists = false;
    
    /**
     * Constructs a new instance which forwards to the JSP corresponding to 
     * the current request's URL.
     */
    public ViewForwardResolution() {
        super((String) null);
    }
    
    /**
     * Constructs a new instance which forwards to the JSP corresponding to the 
     * specified view.
     *  
     * @param viewName the name of the view. The prefix set with {@link #setPrefix(String)} 
     *               will be prepended and the suffix set with {@link #setSuffix(String)} will be 
     *               appended to get the final name of the JSP.
     */
    public ViewForwardResolution(String viewName) {
        super("");
        if (!viewName.startsWith("/")) {
            viewName = "/" + viewName;
        }
        setPath(prefix + viewName + suffix);
    }

    public ViewForwardResolution(Class<? extends ActionBean> beanType) {
        super(beanType);
    }

    public ViewForwardResolution(Class<? extends ActionBean> beanType, String event) {
        super(beanType, event);
    }
    
    protected String extractViewName(String uri, HttpServletRequest request) {
        String contextPath = request.getContextPath();
        uri = StringUtils.removeStart(uri, contextPath);
        return uri.replaceFirst(viewNameSuffixPattern, "");
        
    }
    
    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.getPath() == null) {
            String viewName = extractViewName(request.getRequestURI(), request);
            setPath(prefix + viewName + suffix);
        }
        
        if (checkExists) {
            /*
             * Check if the JSP actually exists. Without this code the SAXWriter
             * will throw an exception if the JSP doesn't exist and we'll get a 500
             * error instead.
             */
            String realPath = request.getSession().getServletContext().getRealPath(getPath());
            if (!new File(realPath).exists()) {
                log.warn("View " + getPath() + " not found");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        super.execute(request, response);
    }

    /**
     * If set to <code>true</code> this class will check if the view JSP 
     * actually exists before forwarding to it. If not set the {@link SAXWriter}
     * will throw an exception if the JSP doesn't exist and we'll get a 500
     * error instead of the more appropriate 404 error. This should only be
     * set for the default {@link ActionBean} (like {@link DefaultActionBean})
     * which handles URLs which haven't been mapped to an explicit 
     * {@link ActionBean}. 
     */
    public ViewForwardResolution checkIfExists(boolean b) {
        this.checkExists = b;
        return this;
    }
    
    public void setPrefix(String prefix) {
        Assert.notNull(prefix);
        this.prefix = prefix;
    }
    
    public void setSuffix(String suffix) {
        Assert.notNull(suffix);
        this.suffix = suffix;
    }
    
    public void setViewNameSuffixPattern(String viewNameSuffixPattern) {
        this.viewNameSuffixPattern = viewNameSuffixPattern;
    }
    
}
