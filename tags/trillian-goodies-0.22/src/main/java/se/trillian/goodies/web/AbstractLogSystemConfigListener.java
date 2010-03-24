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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Abstract {@link ServletContextListener} which automatically loads a logging 
 * system config file based on the current host's host name or a prefix thereof.
 * 
 * @author Niklas Therning
 * @version $Id$
 */
public abstract class AbstractLogSystemConfigListener implements ServletContextListener {
    private final String logSystem;
    private final String base;
    private final String[] extensions;
    
    protected AbstractLogSystemConfigListener(String logSystem, String base, String[] extensions) {
        this.logSystem = logSystem;
        this.base = base;
        this.extensions = extensions;
    }
    
    protected String getRealPath(ServletContext servletContext, String path) throws FileNotFoundException {
        // Interpret location as relative to the web application root directory.
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String realPath = servletContext.getRealPath(path);
        if (realPath == null) {
            throw new FileNotFoundException();
        }
        return realPath;
    }
    
    protected String getConfigFile(ServletContext servletContext, String path) throws FileNotFoundException {
        String location = getRealPath(servletContext, "/WEB-INF/" + path);
        File f = new File(location);
        if (f.isFile() && f.exists()) {
            return f.getAbsolutePath();
        }
        throw new FileNotFoundException();
    }
    
    protected String getDefaultConfigFile(ServletContext servletContext) {
        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            servletContext.log(this.getClass().getName() + ": Could not " 
                    + "determine the name for the current host. Using '"  
                    + hostName + "' instead.");
        }
        
        String hostNamePrefix = hostName;
        while (hostNamePrefix.length() > 0) {
            for (String extension : extensions) {
                try {
                    return getConfigFile(servletContext, base + "-" + hostNamePrefix + "." + extension);
                } catch (FileNotFoundException e) {}
            }
            hostNamePrefix = hostName.substring(0, hostNamePrefix.length() - 1);
        }

        for (String extension : extensions) {
            try {
                return getConfigFile(servletContext, base + "." + extension);
            } catch (FileNotFoundException e) {}
        }

        return null;
    }

    protected abstract void configure(ServletContext servletContext, String logFile);
    
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        String logFile = getDefaultConfigFile(servletContext);
        if (logFile == null) {
            servletContext.log(this.getClass().getName() + ": Could not find " 
                    + "a " + logSystem + " config file. Default configuration " 
                    + "will be used.");
            return;
        }

        servletContext.log(this.getClass().getName() + ": Initializing " 
                + logSystem + " from " + logFile);

        configure(servletContext, logFile);
    }

}
