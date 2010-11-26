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

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Tests {@link RMICacheManagerPeerProviderFactory}.
 *
 * @version $Id$
 */
public class RMICacheManagerPeerProviderFactoryTest extends TestCase {

    public void testParseAddresses() throws Exception {
        List<InetAddress> addresses = RMICacheManagerPeerProviderFactory.parseAddresses("127.0.0.1;127.0.0.2:127.0.0.3, www.google.com");
        assertEquals(4, addresses.size());
        assertTrue(addresses.contains(InetAddress.getByName("127.0.0.1")));
        assertTrue(addresses.contains(InetAddress.getByName("127.0.0.2")));
        assertTrue(addresses.contains(InetAddress.getByName("127.0.0.3")));
        assertTrue(addresses.contains(InetAddress.getByName("www.google.com")));
    }
    
    public void testParsePorts() throws Exception {
        Set<Integer> ports = RMICacheManagerPeerProviderFactory.parsePorts("1,2,3;4: 5 , 7 - 9, 14 - 12");
        assertFalse(ports.contains(0));
        assertTrue(ports.contains(1));
        assertTrue(ports.contains(2));
        assertTrue(ports.contains(3));
        assertTrue(ports.contains(4));
        assertTrue(ports.contains(5));
        assertFalse(ports.contains(6));
        assertTrue(ports.contains(7));
        assertTrue(ports.contains(8));
        assertTrue(ports.contains(9));
        assertFalse(ports.contains(10));
        assertFalse(ports.contains(11));
        assertTrue(ports.contains(12));
        assertTrue(ports.contains(13));
        assertTrue(ports.contains(14));
        assertFalse(ports.contains(15));
        assertEquals(11, ports.size());
        
        try {
            RMICacheManagerPeerProviderFactory.parsePorts("1,65536-65540");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testDistributedCaching() throws Exception {
        CacheManager man1 = new CacheManager(getClass().getResource("ehcache.xml"));
        ((UnicastRMICacheManagerPeerProvider) man1.getCacheManagerPeerProvider("RMI")).getHeartBeatSender().setHeartbeatInterval(1000);
        CacheManager man2 = new CacheManager(getClass().getResource("ehcache.xml"));
        ((UnicastRMICacheManagerPeerProvider) man2.getCacheManagerPeerProvider("RMI")).getHeartBeatSender().setHeartbeatInterval(1000);
        CacheManager man3 = new CacheManager(getClass().getResource("ehcache.xml"));
        ((UnicastRMICacheManagerPeerProvider) man3.getCacheManagerPeerProvider("RMI")).getHeartBeatSender().setHeartbeatInterval(1000);
        
        man1.addCache("cache1");
        man1.addCache("cache2");
        man2.addCache("cache1");
        man2.addCache("cache2");
        man3.addCache("cache1");
        man3.addCache("cache2");
        
        // Wait until the cache managers have found each other
        while (man1.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(man1.getEhcache("cache1")).size() != 2) {
            Thread.sleep(100);
        }
        while (man2.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(man2.getEhcache("cache1")).size() != 2) {
            Thread.sleep(100);
        }
        while (man3.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(man3.getEhcache("cache1")).size() != 2) {
            Thread.sleep(100);
        }
        
        man1.getCache("cache1").put(new Element("o1", "Object from man1"));
        assertNotNull(man1.getCache("cache1").get("o1"));
        assertNotNull(man2.getCache("cache1").get("o1"));
        assertNotNull(man3.getCache("cache1").get("o1"));
        assertEquals("Object from man1", man1.getCache("cache1").get("o1").getObjectValue());
        assertEquals("Object from man1", man2.getCache("cache1").get("o1").getObjectValue());
        assertEquals("Object from man1", man3.getCache("cache1").get("o1").getObjectValue());
        
        man2.getCache("cache1").put(new Element("o2", "Object from man2"));
        assertNotNull(man1.getCache("cache1").get("o2"));
        assertNotNull(man2.getCache("cache1").get("o2"));
        assertNotNull(man3.getCache("cache1").get("o2"));
        assertEquals("Object from man2", man1.getCache("cache1").get("o2").getObjectValue());
        assertEquals("Object from man2", man2.getCache("cache1").get("o2").getObjectValue());
        assertEquals("Object from man2", man3.getCache("cache1").get("o2").getObjectValue());
        
        man3.getCache("cache1").remove("o1");
        assertNull(man1.getCache("cache1").get("o1"));
        assertNull(man2.getCache("cache1").get("o1"));
        assertNull(man3.getCache("cache1").get("o1"));
        
        man1.getCache("cache1").remove("o2");
        assertNull(man1.getCache("cache1").get("o2"));
        assertNull(man2.getCache("cache1").get("o2"));
        assertNull(man3.getCache("cache1").get("o2"));
        
        man1.shutdown();
        man2.shutdown();
        man3.shutdown();
    }
}
