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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.extras.DOMConfigurator;

/**
 * {@link ServletContextListener} which automatically loads a Log4J config file 
 * based on the current host's host name or a prefix thereof.
 * <p>
 * This class will first look for a file named 
 * <code>/WEB-INF/log4j-<i>&lt;hostname&gt;</i>.(properties|xml)</code>. 
 * If no such file is found it will look for a file named 
 * <code>/WEB-INF/log4j-<i>&lt;hostname-prefix&gt;</i>.(properties|xml)</code> where 
 * <i>hostname-prefix</i> is a prefix of the host name. The file with the 
 * longest prefix of the host name in its name will be loaded.
 * </p>
 * <p>
 * If no file is found containing the host name or a prefix of it the file named
 * <code>/WEB-INF/log4j.(properties|xml)</code> will be used. If no such file 
 * exists Log4j will be initialized using its default look up strategy.
 * </p>
 * <p>
 * XML-based Log4j files will be loaded using the {@link DOMConfigurator}
 * class provided by the 
 * <a href="http://logging.apache.org/log4j/companions/extras/">Apache Log4j Extras project</a>.
 * The Extras project provides additional appenders, filters, formatters and 
 * other components for log4j 1.2.
 * </p>
 * <p>
 * If no reload time has been specified as servlet context parameter 
 * ({@link #REFRESH_INTERVAL_PARAM}), no reloading will be used.
 * </p>
 * 
 * @author Henric Müller
 * @version $Id$
 */
public class Log4jConfigListener extends AbstractLogSystemConfigListener {
    
    public Log4jConfigListener() {
        super("Log4j", "log4j", new String[] {"properties", "xml"});
    }
    
    /**
     * Parameter specifying the refresh interval for checking the log4j config file
     */
    public static final String REFRESH_INTERVAL_PARAM = "log4jRefreshInterval";
    
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        servletContext.log(this.getClass().getName() + ": Shutting down Log4J");
        LogManager.shutdown();
    }

    @Override
    protected void configure(ServletContext servletContext, String logFile) {
        /*
         * Check whether refresh interval was specified.
         */
        String intervalString = servletContext.getInitParameter(REFRESH_INTERVAL_PARAM);
        if (intervalString != null) {
            /* 
             * Initialize with refresh interval, i.e. with Log4J's watchdog thread,
             * checking the file in the background.
             */
            try {
                long refreshInterval = Long.parseLong(intervalString);
                if (logFile.toLowerCase().endsWith(".xml")) {
                    DOMConfigurator.configureAndWatch(logFile, refreshInterval);
                } else {
                    PropertyConfigurator.configureAndWatch(logFile, refreshInterval);
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Invalid '" + REFRESH_INTERVAL_PARAM + "' parameter: "
                                + ex.getMessage());
            }
        } else {
            /*
             * Initialize without refresh check, i.e. without Log4J's watchdog thread.
             */
            if (logFile.toLowerCase().endsWith(".xml")) {
                DOMConfigurator.configure(logFile);
            } else {
                PropertyConfigurator.configure(logFile);
            }
        }
    }

}
