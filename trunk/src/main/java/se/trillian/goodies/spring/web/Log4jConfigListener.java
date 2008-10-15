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
package se.trillian.goodies.spring.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.springframework.util.Log4jConfigurer;
import org.springframework.web.util.Log4jWebConfigurer;
import org.springframework.web.util.WebUtils;

/**
 * Servlet context listener which automatically loads a Log4J config file.
 * <p>
 * This class will first look for a file named 
 * <code>/WEB-INF/log4j-<i>&lt;hostname&gt;</i>.properties</code>. If no such
 * file is found it will look for a file named <code>/WEB-INF/log4j.properties</code>. 
 * If no such file exist Log4j will be initialized using {@link BasicConfigurator}. 
 * If no reload time has been specified as servlet context parameter 
 * ({@link Log4jWebConfigurer.REFRESH_INTERVAL_PARAM}), no reloading will be used.
 * 
 * @author Henric Müller
 * @version $Id$
 */
public class Log4jConfigListener implements ServletContextListener {

    private static Log log = LogFactory.getLog(Log4jConfigListener.class);
    
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        servletContext.log("Shutting down Log4J");
        Log4jConfigurer.shutdownLogging();
        
    }
    
    private String getDefaultLog4jFile(ServletContext servletContext) {
        // Try with log4j.<hostname>.properties in WEB-INF directory
        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            log.warn("Could not determine the name for the current host. Using '"  + hostName + "'.");
        }
        try {
            String location = WebUtils.getRealPath(servletContext, "/WEB-INF/log4j-" + hostName + ".properties");
            File f = new File(location);
            if (f.isFile() && f.exists()) {
                return location;
            }
        } catch (FileNotFoundException nfe) {
        }
        try {
            String location = WebUtils.getRealPath(servletContext, "/WEB-INF/log4j.properties");
            File f = new File(location);
            if (f.isFile() && f.exists()) {
                return location;
            }
        } catch (FileNotFoundException e) {
            log.warn("Could not find default log4j.properties file.");
        }
        return null;
    }

        
    public void contextInitialized(ServletContextEvent event) {
        BasicConfigurator.configure();
        ServletContext servletContext = event.getServletContext();
        String location = getDefaultLog4jFile(servletContext);
        if (location == null) {
            log.warn("Could not find a log4j.properties. Default Log4J configuration will be used.");
            return;
        }
        try {

            // Write log message to server log.
            servletContext.log("Initializing Log4J from [" + location + "]");

            // Check whether refresh interval was specified.
            String intervalString = servletContext.getInitParameter(Log4jWebConfigurer.REFRESH_INTERVAL_PARAM);
            if (intervalString != null) {
                // Initialize with refresh interval, i.e. with Log4J's
                // watchdog thread,
                // checking the file in the background.
                try {
                    long refreshInterval = Long.parseLong(intervalString);
                    Log4jConfigurer.initLogging(location, refreshInterval);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid 'log4jRefreshInterval' parameter: "
                                    + ex.getMessage());
                }
            } else {
                // Initialize without refresh check, i.e. without Log4J's
                // watchdog thread.
                Log4jConfigurer.initLogging(location);
            }
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException(
                    "Invalid 'log4jConfigLocation' parameter: "
                            + ex.getMessage());
        }
        
    }

}
