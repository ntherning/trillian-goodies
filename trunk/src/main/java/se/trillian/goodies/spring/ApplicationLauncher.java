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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Launches a standalone (not running in a web container) application by loading
 * a set of Spring configuration files into a single context. All non-lazy 
 * singleton beans in those files will be created automatically. The Spring 
 * files may be located in the classpath if they are prefixed with the 
 * <code>classptah*:</code> prefix. Execute it like this:
 * <pre>
 *      ApplicationLauncher [logback-config-file] [context1.xml [context2.xml ...]]
 * </pre>
 * <p>
 * If no Logback configuration file is specified this class will first look for a 
 * file named <code>logback-<i>&lt;hostname&gt;</i>.xml</code> in the 
 * working directory. If no such file is found it will look for a file named 
 * <code>logback-<i>&lt;hostname-prefix&gt;</i>.xml</code> where 
 * <i>hostname-prefix</i> is a prefix of the host name. If no such file is found 
 * it will look for a file named <code>logback-<i>&lt;hostname-prefix&gt;</i>{X...}.xml</code>.
 * The file with the longest prefix of the host name in its name will be loaded.
 * E.g. if the hostname is <code>s10</code> the following files will be tried:
 * <code>logback-s10.xml</code>, <code>logback-s1.xml</code>, <code>logback-s1X.xml</code>,
 * <code>logback-s.xml</code>, <code>logback-sXX.xml</code>, <code>logback-.xml</code>,
 * <code>logback-XXX.xml</code>
 * </p>
 * <p>
 * If no file is found containing the host name or a prefix of it the file named
 * <code>logback.xml</code> will be used. If no such file exists Logback will 
 * be initialized using {@link BasicConfigurator}.
 * </p>
 * <p>
 * If no Spring context files have been specified this class will load all 
 * Spring files matching <code>classpath*:/spring/*.xml</code>.
 * </p> 
 *
 * @author Niklas Therning
 * @version $Id$
 */
public class ApplicationLauncher {
    private static final Logger log = LoggerFactory.getLogger(ApplicationLauncher.class);
    private static final String hostName;
    private static final String fullHostName;
    
    static {
        String name = "localhost";
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            log.warn("Could not determine the name for the current host. Using '" 
                    + name + "'.");
        }
        fullHostName = name;
        int dotIndex = fullHostName.indexOf('.');
        if (dotIndex != -1) {
            hostName = fullHostName.substring(0, dotIndex);
        } else {
            hostName = fullHostName;
        }
    }
    
    private AbstractXmlApplicationContext context = null;
    
    private static void printUsage(String msg) {
        if (msg != null) {
            System.err.println(msg);
        }
        System.err.println("Usage:");
        System.err.println("ApplicationLauncher [logback-config-file] [context1.xml [context2.xml ...]] ...");
    }
    
    public static final void main(String[] args) throws Throwable {
        /*
         * Use BasicConfigurator temporarily to enable logging before the 
         * logback file has been read
         */
        BasicConfigurator.configureDefaultContext();
        
        boolean useWrapper = false;
        try {
            useWrapper = WrapperManager.isControlledByNativeWrapper();
        } catch (Error e) {
            if (!(e instanceof NoClassDefFoundError)) {
                throw e;
            }
        }
        
        if (useWrapper) {
            WrapperFacade.launch(new ApplicationLauncher(), args);
        } else {
            new ApplicationLauncher().start(args);
        }
    }
    
    public Integer start(String[] args) {
        String logFile = null;
        ArrayList<String> springFiles = new ArrayList<String>();
        try {
            int i = 0;
            if (i < args.length && args[i].toLowerCase().matches("logback.*\\.xml")) {
                logFile = args[i++];
            }
            
            while (i < args.length) {
                springFiles.add(args[i++]);
            }
        } catch (Throwable t) {
            printUsage(t.getMessage());
            return new Integer(1);
        }
        
        if (logFile == null) {
            logFile = getDefaultLogbackFile();
        }
        
        try {
            /*
             * Initialize the logback logging system.
             */
            if (logFile != null) {
                log.info("Configuring logback from file '" + logFile + "'");
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                lc.reset();
                lc.putProperty("se.trillian.goodies.hostname", hostName);
                lc.putProperty("se.trillian.goodies.hostname.full", fullHostName);
                
                try {
                    JoranConfigurator configurator = new JoranConfigurator();
                    configurator.setContext(lc);
                    configurator.doConfigure(logFile);
                } catch (JoranException je) {
                    StatusPrinter.print(lc);
                } 
            }
        } catch (Throwable t) {
            log.error("Failed to initialize logback logging system: " + t.getMessage(), t);
            return new Integer(2);
        }
            
        try {
            /*
             * Load the Spring bean configuration files.
             */
            if (springFiles.isEmpty()) {
                springFiles.add("classpath*:/spring/*.xml");
            }
            log.info("Loading spring files: " + springFiles);
            context = new FileSystemXmlApplicationContext(springFiles.toArray(new String[0]), false);
            context.refresh();
        } catch (Throwable t) {
            log.error("Unable to load Spring configuration files", t);
            return new Integer(3);
        }
        
        return null;
    }
    
    public AbstractXmlApplicationContext getContext() {
        return context;
    }
    
    private String getLogbackFile(String path) throws FileNotFoundException {
        File f = new File(path);
        if (f.isFile() && f.exists()) {
            return f.getAbsolutePath();
        }
        throw new FileNotFoundException();
    }
    
    private String getDefaultLogbackFile() {
        String prefix = hostName;
        String suffix = "";
        while (prefix.length() > 0) {
            try {
                return getLogbackFile("logback-" + prefix + ".xml");
            } catch (FileNotFoundException e) {}
            if (suffix.length() > 0) {
                try {
                    return getLogbackFile("logback-" + prefix + suffix + ".xml");
                } catch (FileNotFoundException e) {}
            }
            prefix = hostName.substring(0, prefix.length() - 1);
            suffix = suffix + "X";
        }

        try {
            return getLogbackFile("logback.xml");
        } catch (FileNotFoundException e) {}

        return null;
    }
    
    public int stop(int exitCode) {
        // Shutdown logback before closing the Spring context to 
        // avoid noisy ERROR logging during shutdown
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();
        
        if (context != null) {
            context.close();
        }
        
        return exitCode;
    }
}

class WrapperFacade {
    static void launch(final ApplicationLauncher launcher, String[] args) {
        WrapperManager.start(new WrapperListener() {
            public int stop(int exitCode) {
                return launcher.stop(exitCode);
            }
            public Integer start(String[] args) {
                return launcher.start(args);
            }
            public void controlEvent(int event) {
                if (   event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT
                    && WrapperManager.isLaunchedAsService()) {
                        // Ignore
                } else {
                    WrapperManager.stop(0);
                }
            }
        }, args);
    }
}
