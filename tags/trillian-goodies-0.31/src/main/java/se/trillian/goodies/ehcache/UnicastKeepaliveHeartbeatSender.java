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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @version $Id$
 */
public class UnicastKeepaliveHeartbeatSender {
    
    private static final Logger LOG = LoggerFactory.getLogger(UnicastKeepaliveHeartbeatSender.class);

    public static final int DEFAULT_HEARTBEAT_INTERVAL = 5000;
    private static final int MAXIMUM_PEERS_PER_SEND = 150;
    
    private static AtomicInteger threadCount = new AtomicInteger();
    
    private int heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    private CacheManager cacheManager;
    private List<InetAddress> peerAddresses; 
    private Set<Integer> peerPorts;
    private DatagramChannel channel;
    private SenderThread serverThread;
    private boolean stopped;

    public UnicastKeepaliveHeartbeatSender(CacheManager cacheManager,
            DatagramChannel channel,
            List<InetAddress> peerAddresses, Set<Integer> peerPorts) {
        
        this.cacheManager = cacheManager;
        this.channel = channel;
        this.peerAddresses = peerAddresses;
        this.peerPorts = peerPorts;
    }

    public final void init() {
        LOG.debug("init called");
        serverThread = new SenderThread();
        LOG.debug("Starting sender thread");
        serverThread.start();
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    
    public final synchronized void dispose() {
        LOG.debug("dispose called");
        stopped = true;
        notifyAll();
        serverThread.interrupt();
    }
    
    private final class SenderThread extends Thread {

        private List<byte[]> compressedUrlListList = new ArrayList<byte[]>();
        private int cachePeersHash;

        public SenderThread() {
            super("Unicast Heartbeat Sender Thread #" + threadCount.incrementAndGet());
            setDaemon(true);
        }

        public final void run() {
            InetSocketAddress localAddress = (InetSocketAddress) channel.socket().getLocalSocketAddress();
            
            while (!stopped) {
                try {
                    
                    while (!stopped) {
                        List<byte[]> buffers = createCachePeersPayload();
                        for (byte[] buffer : buffers) {
                            for (InetAddress peerAddress : peerAddresses) {
                                for (int peerPort : peerPorts) {
                                    InetSocketAddress addr = new InetSocketAddress(peerAddress, peerPort);
                                    if (!localAddress.equals(addr)) {
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("Sending rmiUrls to " + addr);
                                        }
                                        channel.send(ByteBuffer.wrap(buffer), addr);
                                    }
                                }
                            }
                        }
                        try {
                            synchronized (this) {
                                wait(heartbeatInterval);
                            }
                        } catch (InterruptedException e) {
                            if (!stopped) {
                                LOG.error("Error receiving heartbeat. Initial cause was " + e.getMessage(), e);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.debug("Error on socket", e);
                } catch (Throwable e) {
                    LOG.info("Unexpected throwable in run thread. Continuing..." + e.getMessage(), e);
                }
                
                if (!stopped) {
                    try {
                        sleep(heartbeatInterval);
                    } catch (InterruptedException e) {
                        LOG.error("Sleep after error interrupted. Initial cause was " + e.getMessage(), e);
                    }
                }
            }
        }

        /**
         * Creates a gzipped payload.
         * <p/>
         * The last gzipped payload is retained and only recalculated if the list of cache peers
         * has changed.
         *
         * @return a gzipped byte[]
         */
        private List<byte[]> createCachePeersPayload() {
            List localCachePeers = cacheManager.getCachePeerListener("RMI").getBoundCachePeers();
            int newCachePeersHash = localCachePeers.hashCode();
            if (cachePeersHash != newCachePeersHash) {
                cachePeersHash = newCachePeersHash;

                compressedUrlListList = new ArrayList<byte[]>();
                while (localCachePeers.size() > 0) {
                    int endIndex = Math.min(localCachePeers.size(), MAXIMUM_PEERS_PER_SEND);
                    List localCachePeersSubList = localCachePeers.subList(0, endIndex);
                    localCachePeers = localCachePeers.subList(endIndex, localCachePeers.size());

                    byte[] uncompressedUrlList = PayloadUtil.assembleUrlList(localCachePeersSubList);
                    byte[] compressedUrlList = PayloadUtil.gzip(uncompressedUrlList);
                    if (compressedUrlList.length > PayloadUtil.MTU) {
                        LOG.error("Heartbeat is not working. Configure fewer caches for replication. " +
                                "Size is " + compressedUrlList.length + " but should be no greater than" +
                                PayloadUtil.MTU);
                    }
                    compressedUrlListList.add(compressedUrlList);
                }
            }
            return compressedUrlListList;
        }

    }
}
