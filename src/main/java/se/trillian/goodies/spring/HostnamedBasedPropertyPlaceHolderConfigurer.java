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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

/**
 * Spring {@link PropertyPlaceholderConfigurer} which reads some default property
 * files from the classpath. The following properties files will be loaded in 
 * the listed order:
 * <ol>
 *      <li><i>&lt;prefix&gt;</i>/spring-defaults.properties</li>
 *      <li><i>&lt;prefix&gt;</i>/spring-defaults-<i>&lt;hostname&gt;</i>.properties</li>
 *      <li><i>&lt;prefix&gt;</i>/spring.properties</li>
 *      <li><i>&lt;prefix&gt;</i>/spring-<i>&lt;hostname&gt;</i>.properties</li>
 * </ol>
 * <p>
 * If the same property occurs in more than one of the files the one loaded 
 * last will be used. If any of the files listed above is missing it will be 
 * skipped.
 * </p>
 * <p>
 * <i>&lt;prefix&gt;</i> is <code>/spring</code> by default.
 * </p>
 * 
 * @author Niklas Therning
 * @version $Id$
 */
public class HostnamedBasedPropertyPlaceHolderConfigurer extends
                PropertyPlaceholderConfigurer {

    private static final Log log = LogFactory.getLog(HostnamedBasedPropertyPlaceHolderConfigurer.class);
    
    private String prefix = "/spring";
    
    public void setPrefix(String prefix) {
        Assert.notNull(prefix);
        Assert.hasLength(prefix);
        prefix = prefix.replaceAll("\\\\", "/");
        prefix = prefix.replaceAll("/+$", "");
        this.prefix = prefix;
    }

    @Override
    protected Properties mergeProperties() throws IOException {
        Properties properties = new Properties();
        
        String hostName = "localhost";
        try {
            hostName = getHostName();
        } catch (UnknownHostException uhe) {
            log.warn("Could not determine the name for the current host. Using '" 
                    + hostName + "'.");
        }
        
        loadProperties("spring-defaults.properties", properties);
        loadProperties("spring-defaults-" + hostName + ".properties", properties);
        loadProperties("spring.properties", properties);
        loadProperties("spring-" + hostName + ".properties", properties);
        
        properties.putAll(super.mergeProperties());
        
        return properties;
    }

    protected String getHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
    
    private void loadProperties(String file, Properties properties) {
        ClassPathResource res = new ClassPathResource(prefix + "/" + file);
        log.info("Reading properties from " + res.getPath());
        InputStream is = null;
        try {
            is = res.getInputStream();
            Properties temp = new Properties();
            temp.load(is);
            properties.putAll(temp);
        } catch (FileNotFoundException fnfe) {
            log.info("Properties file " + res.getPath() + " not found. Skipping.");
        } catch (IOException ie) {
            log.error("Could not read properties file " + res.getPath(), ie);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {}
            }
        }
    }
    
}
