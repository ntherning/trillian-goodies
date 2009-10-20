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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.extras.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * Launches a standalone (not running in a web container) application by loading
 * a set of Spring configuration files into a single context. All non-lazy 
 * singleton beans in those files will be created automatically. The Spring 
 * files may be located in the classpath if they are prefixed with the 
 * <code>classptah*:</code> prefix. Execute it like this:
 * <pre>
 *      ApplicationLauncher [[-d log-file-reload-delay-in-secs] log4j-properties-file]
 *                                    [context1.xml [context2.xml ...]]
 * </pre>
 * <p>
 * If no Log4j configuration file is specified this class will first look for a 
 * file named <code>log4j-<i>&lt;hostname&gt;</i>.(properties|xml)</code> in the 
 * working directory. If no such file is found it will look for a file named 
 * <code>log4j-<i>&lt;hostname-prefix&gt;</i>.(properties|xml)</code> where 
 * <i>hostname-prefix</i> is a prefix of the host name. The file with the 
 * longest prefix of the host name in its name will be loaded.
 * </p>
 * <p>
 * If no file is found containing the host name or a prefix of it the file named
 * <code>log4j.(properties|xml)</code> will be used. If no such file exists Log4j will 
 * be initialized using {@link BasicConfigurator}.
 * </p>
 * <p>
 * XML-based Log4j files will be loaded using the {@link DOMConfigurator}
 * class provided by the 
 * <a href="http://logging.apache.org/log4j/companions/extras/">Apache Log4j Extras project</a>.
 * The Extras project provides additional appenders, filters, formatters and 
 * other components for log4j 1.2.
 * </p>
 * <p>
 * Please note that if a Log4j file is specified it must have either the 
 * <code>.properties</code> or <code>.xml</code> file extension.
 * </p>
 * <p>
 * If no reload time has been given 5 seconds will be used. Use 0 to disable 
 * reloading completely.
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
    
    private AbstractXmlApplicationContext context = null;
    
    private static void printUsage(String msg) {
        if (msg != null) {
            System.err.println(msg);
        }
        System.err.println("Usage:");
        System.err.println("ApplicationLauncher [[-d log-file-reload-delay-in-secs] "
                        + "<log4j-properties-file>] [context1.xml [context2.xml ...]] ...");
    }
    
    public static final void main(String[] args) throws Throwable {
        /*
         * Use BasicConfigurator temporarily to enable logging before the 
         * log4j.properties file has been read
         */
        BasicConfigurator.configure();
        
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
        long delay = 5;
        try {
            int i = 0;
            if (i < args.length && args[i].equals("-d")) {
                i++;
                delay = Long.parseLong(args[i++]);
                if (delay < 0) {
                    throw new IllegalArgumentException("Specified delay < 0");
                }
            }
            if (i < args.length && args[i].toLowerCase().matches(".*\\.(properties|xml)")) {
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
            logFile = getDefaultLog4jFile();
        }
        
        try {
            /*
             * Initialize the log4j logging system.
             */
            if (logFile != null) {
                log.info("Configuring log4j from file '" + logFile + "'");
                if (delay > 0) {
                    log.info("Using a reload interval of " + delay + " second(s)");
                    if (logFile.toLowerCase().endsWith(".xml")) {
                        DOMConfigurator.configureAndWatch(logFile, delay * 1000);
                    } else {
                        PropertyConfigurator.configureAndWatch(logFile, delay * 1000);
                    }
                } else {
                    log.info("Log4j will not reload the file if changed");
                    if (logFile.toLowerCase().endsWith(".xml")) {
                        DOMConfigurator.configure(logFile);
                    } else {
                        PropertyConfigurator.configure(logFile);
                    }
                }
            }
        } catch (Throwable t) {
            log.error("Failed to initialize log4j logging system: " + t.getMessage(), t);
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
            context.addBeanFactoryPostProcessor(new HostNameBasedPropertyPlaceHolderConfigurer());
            context.refresh();
        } catch (Throwable t) {
            log.error("Unable to load Spring configuration files", t);
            return new Integer(3);
        }
        
        return null;
    }

    private String getLog4jFile(String path) throws FileNotFoundException {
        File f = new File(path);
        if (f.isFile() && f.exists()) {
            return f.getAbsolutePath();
        }
        throw new FileNotFoundException();
    }
    
    private String getDefaultLog4jFile() {
        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            log.warn("Could not determine the name for the current host. Using '" 
                    + hostName + "'.");
        }
        
        String prefix = hostName;
        while (prefix.length() > 0) {
            try {
                return getLog4jFile("log4j-" + prefix + ".properties");
            } catch (FileNotFoundException e) {}
            try {
                return getLog4jFile("log4j-" + prefix + ".xml");
            } catch (FileNotFoundException e) {}
            prefix = hostName.substring(0, prefix.length() - 1);
        }

        try {
            return getLog4jFile("log4j.properties");
        } catch (FileNotFoundException e) {}
        try {
            return getLog4jFile("log4j.xml");
        } catch (FileNotFoundException e) {}

        return null;
    }
    
    public int stop(int exitCode) {
        
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
