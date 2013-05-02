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
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CachePeer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @version $Id$
 */
public class UnicastKeepaliveHeartbeatReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(UnicastKeepaliveHeartbeatReceiver.class);

    private static AtomicInteger threadCount = new AtomicInteger();

    private ExecutorService processingThreadPool;
    private Set rmiUrlsProcessingQueue = Collections.synchronizedSet(new HashSet());
    private ReceiverThread receiverThread;
    private DatagramChannel channel;
    private boolean stopped;
    private final UnicastRMICacheManagerPeerProvider peerProvider;
    
    public UnicastKeepaliveHeartbeatReceiver(
            UnicastRMICacheManagerPeerProvider peerProvider,
            DatagramChannel channel) {

        this.peerProvider = peerProvider;
        this.channel = channel;
    }

    final void init() throws IOException {
        LOG.debug("init called");
        receiverThread = new ReceiverThread();
        receiverThread.start();
        processingThreadPool = Executors.newCachedThreadPool();
    }

    public final void dispose() {
        LOG.debug("dispose called");
        processingThreadPool.shutdownNow();
        stopped = true;
        receiverThread.interrupt();
    }

    private final class ReceiverThread extends Thread {

        public ReceiverThread() {
            super("Unicast Heartbeat Receiver Thread #" + threadCount.incrementAndGet());
            setDaemon(true);
        }

        public final void run() {
            byte[] buf = new byte[PayloadUtil.MTU];
            Selector selector = null;
            try {
                selector = Selector.open();
                channel.register(selector, SelectionKey.OP_READ);
                while (!stopped) {
                    try {
                        int n = selector.select();
                        if (n > 0) {
                            selector.selectedKeys().clear();
                            channel.receive(ByteBuffer.wrap(buf));
                            processPayload(buf);
                        }
                    } catch (IOException e) {
                        if (!stopped) {
                            LOG.error("Error receiving heartbeat. " + e.getMessage() +
                                    ". Initial cause was " + e.getMessage(), e);
                        }
                    }
                }
            } catch (Throwable t) {
                LOG.error("Unicast receiver thread caught throwable. Cause was " + t.getMessage() + ". Continuing...");
            }
        }

        private void processPayload(byte[] compressedPayload) {
            byte[] payload = PayloadUtil.ungzip(compressedPayload);
            String rmiUrls = new String(payload);
            if (self(rmiUrls)) {
                return;
            }
            rmiUrls = rmiUrls.trim();
            if (LOG.isDebugEnabled()) {
                LOG.debug("rmiUrls received " + rmiUrls);
            }
            processRmiUrls(rmiUrls);
        }

        /**
         * This method forks a new executor to process the received heartbeat in a thread pool.
         * That way each remote cache manager cannot interfere with others.
         * <p/>
         * In the worst case, we have as many concurrent threads as remote cache managers.
         *
         * @param rmiUrls
         */
        private void processRmiUrls(final String rmiUrls) {
            if (rmiUrlsProcessingQueue.contains(rmiUrls)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("We are already processing these rmiUrls. Another heartbeat came before we finished: " + rmiUrls);
                }
                return;
            }

            if (processingThreadPool == null) {
                return;
            }

            processingThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        // Add the rmiUrls we are processing.
                        rmiUrlsProcessingQueue.add(rmiUrls);
                        for (StringTokenizer stringTokenizer = new StringTokenizer(rmiUrls,
                                PayloadUtil.URL_DELIMITER); stringTokenizer.hasMoreTokens();) {
                            if (stopped) {
                                return;
                            }
                            String rmiUrl = stringTokenizer.nextToken();
                            registerNotification(rmiUrl);
                            if (!peerProvider.getPeerUrls().containsKey(rmiUrl)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Aborting processing of rmiUrls since failed to add rmiUrl: " + rmiUrl);
                                }
                                return;
                            }
                        }
                    } finally {
                        // Remove the rmiUrls we just processed
                        rmiUrlsProcessingQueue.remove(rmiUrls);
                    }
                }
            });
        }


        /**
         * @param rmiUrls
         * @return true if our own hostname and listener port are found in the list. This then means we have
         *         caught our onw multicast, and should be ignored.
         */
        private boolean self(String rmiUrls) {
            CacheManager cacheManager = peerProvider.getCacheManager();
            CacheManagerPeerListener cacheManagerPeerListener = cacheManager.getCachePeerListener("RMI");
            if (cacheManagerPeerListener == null) {
                return false;
            }
            List boundCachePeers = cacheManagerPeerListener.getBoundCachePeers();
            if (boundCachePeers == null || boundCachePeers.size() == 0) {
                return false;
            }
            CachePeer peer = (CachePeer) boundCachePeers.get(0);
            String cacheManagerUrlBase = null;
            try {
                cacheManagerUrlBase = peer.getUrlBase();
            } catch (RemoteException e) {
                LOG.error("Error geting url base");
            }
            int baseUrlMatch = rmiUrls.indexOf(cacheManagerUrlBase);
            return baseUrlMatch != -1;
        }

        private void registerNotification(String rmiUrl) {
            peerProvider.registerPeer(rmiUrl);
        }

    }

}
