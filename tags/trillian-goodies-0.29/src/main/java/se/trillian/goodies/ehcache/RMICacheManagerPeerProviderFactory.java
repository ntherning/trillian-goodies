/*
 * Copyright (c) 2004-2010, Trillian AB. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or 
 * without modification, are prohibited without specific prior 
 * written permission from Trillian AB (http://www.trillian.se).
 *
 * This notice and attribution to Trillian AB may not be removed.
 */
package se.trillian.goodies.ehcache;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.util.PropertyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a factory based on RMI using unicast discovery. 
 */
public class RMICacheManagerPeerProviderFactory extends net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheManagerPeerProviderFactory.class);

    private static final String PEER_DISCOVERY = "peerDiscovery";
    private static final String UNICAST_PEER_DISCOVERY = "unicast";
    
    private static final String HOST_NAME = "hostName";
    private static final String PEER_ADDRESSES = "peerAddresses";
    private static final String PEER_PORTS = "peerPorts";

    public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties)
            throws CacheException {

        String peerDiscovery = PropertyUtil.extractAndLogProperty(PEER_DISCOVERY, properties);
        if (peerDiscovery.equalsIgnoreCase(UNICAST_PEER_DISCOVERY)) {
            try {
                return createSingleCastConfiguredCachePeerProvider(cacheManager, properties);
            } catch (IOException e) {
                throw new CacheException("Could not create CacheManagerPeerProvider. Initial cause was " + e.getMessage(), e);
            }
        }
        return super.createCachePeerProvider(cacheManager, properties);
    }

    protected CacheManagerPeerProvider createSingleCastConfiguredCachePeerProvider(CacheManager cacheManager,
                                                                                      Properties properties) throws IOException {
        String hostName = PropertyUtil.extractAndLogProperty(HOST_NAME, properties);
        InetAddress hostAddress = null;
        if (hostName != null && hostName.length() != 0) {
            hostAddress = InetAddress.getByName(hostName);
            if (hostName.equals("localhost")) {
                LOG.warn("Explicitly setting the hostname to 'localhost' is not recommended. "
                        + "It will only work if all CacheManager peers are on the same machine.");
            }
        }


        List<InetAddress> peerAddresses = parseAddresses(PropertyUtil.extractAndLogProperty(PEER_ADDRESSES, properties));
        Set<Integer> peerPorts = parsePorts(PropertyUtil.extractAndLogProperty(PEER_PORTS, properties));
        
        return new UnicastRMICacheManagerPeerProvider(cacheManager, peerAddresses, peerPorts, hostAddress);
    }

    protected static List<InetAddress> parseAddresses(String p) throws UnknownHostException {
        List<InetAddress> addresses = new ArrayList<InetAddress>();
        for (String s : p.split(",|;|:")) {
            addresses.add(InetAddress.getByName(s.trim()));
        }
        return addresses;
    }
    
    protected static Set<Integer> parsePorts(String p) throws UnknownHostException {
        Set<Integer> ports = new TreeSet<Integer>();
        for (String s : p.split(",|;|:")) {
            String[] t = s.split("-");
            if (t.length > 2) {
                throw new IllegalArgumentException("More than two ports specified in interval '" + s + "'");
            }
            if (t.length == 2) {
                int min = Integer.parseInt(t[0].trim());
                int max = Integer.parseInt(t[1].trim());
                if (min > max) {
                    int tmp = min;
                    min = max;
                    max = tmp;
                }
                for (int i = min; i <= max; i++) {
                    ports.add(i);
                }
            } else {
                ports.add(Integer.parseInt(s.trim()));
            }
        }
        for (int port : ports) {
            if (port > 0xffff) {
                throw new IllegalArgumentException("Port '" + port + "' > maximum port numer (65535)");
            }
        }
        return ports;
    }
}
