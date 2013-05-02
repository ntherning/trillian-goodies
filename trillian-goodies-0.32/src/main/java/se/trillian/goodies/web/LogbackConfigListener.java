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

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * {@link ServletContextListener} which automatically loads a Logback config file 
 * based on the current host's host name or a prefix thereof.
 * <p>
 * This class will first look for a file named 
 * <code>/WEB-INF/logback-<i>&lt;hostname&gt;</i>.xml</code>. 
 * If no such file is found it will look for a file named 
 * <code>/WEB-INF/logback-<i>&lt;hostname-prefix&gt;</i>.xml</code> where 
 * <i>hostname-prefix</i> is a prefix of the host name. The file with the 
 * longest prefix of the host name in its name will be loaded.
 * </p>
 * <p>
 * If no file is found containing the host name or a prefix of it the file named
 * <code>/WEB-INF/logback.xml</code> will be used. If no such file 
 * exists Logback will be initialized using its default look up strategy.
 * </p>
 * 
 * @author Niklas Therning
 * @version $Id$
 */
public class LogbackConfigListener extends AbstractLogSystemConfigListener {
    
    public LogbackConfigListener() {
        super("Logback", "logback", new String[] {"xml"});
    }
    
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        servletContext.log(LogbackConfigListener.class.getName() + ": Shutting down Logback");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();
    }
    
    @Override
    protected void configure(String logFile) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();
        lc.putProperty("se.trillian.goodies.hostname", getHostName());
        lc.putProperty("se.trillian.goodies.hostname.full", getFullHostName());
        
        try {
           JoranConfigurator configurator = new JoranConfigurator();
           configurator.setContext(lc);
           configurator.doConfigure(logFile);
        } catch (JoranException je) {
           StatusPrinter.print(lc);
        } 
    }

}
