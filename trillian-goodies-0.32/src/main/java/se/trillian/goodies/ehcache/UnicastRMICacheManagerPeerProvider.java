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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.distribution.RMICacheManagerPeerProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A peer provider which discovers peers using unicast.
 * <p>
 * The list of CachePeers is maintained via heartbeats. rmiUrls are looked up using RMI and converted to CachePeers on
 * registration. On lookup any stale references are removed.
 */
public final class UnicastRMICacheManagerPeerProvider extends RMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    /**
     * One tenth of a second, in ms
     */
    protected static final int SHORT_DELAY = 100;

    private static final Logger LOG = LoggerFactory.getLogger(UnicastRMICacheManagerPeerProvider.class);


    private final UnicastKeepaliveHeartbeatReceiver heartBeatReceiver;
    private final UnicastKeepaliveHeartbeatSender heartBeatSender;

    private DatagramChannel channel;

    /**
     * Creates and starts a single cast peer provider
     *
     * @param peerAddresses list of addresses which peers may be listening on.
     * @param peerPorts    list of ports which peers may be listening on.
     * @param hostAddress the address of the interface to use for sending and receiving packets. May be null.
     */
    public UnicastRMICacheManagerPeerProvider(CacheManager cacheManager, List<InetAddress> peerAddresses,
            Set<Integer> peerPorts, InetAddress hostAddress) {
        super(cacheManager);

        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open DatagramChannel");
        }
        for (int p : peerPorts) {
            try {
                channel.socket().bind(new InetSocketAddress(hostAddress, p));
                break;
            } catch (SocketException e) {
                LOG.debug("Failed to bind to port " + p + ": " + e.getMessage());
            }
        }
        if (!channel.socket().isBound()) {
            throw new RuntimeException("Failed to bind to any of the ports in " + peerPorts);
        }

        LOG.debug("Bound to " + channel.socket().getLocalSocketAddress());
        
        heartBeatReceiver = new UnicastKeepaliveHeartbeatReceiver(this, channel);
        heartBeatSender = new UnicastKeepaliveHeartbeatSender(cacheManager, channel, peerAddresses, peerPorts);
    }

    Map getPeerUrls() {
        return super.peerUrls;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void init() throws CacheException {
        try {
            heartBeatReceiver.init();
            heartBeatSender.init();
        } catch (IOException exception) {
            LOG.error("Error starting heartbeat. Error was: " + exception.getMessage(), exception);
            throw new CacheException(exception.getMessage());
        }
    }

    /**
     * Register a new peer, but only if the peer is new, otherwise the last seen timestamp is updated.
     * <p/>
     * This method is thread-safe. It relies on peerUrls being a synchronizedMap
     *
     * @param rmiUrl
     */
    public final void registerPeer(String rmiUrl) {
        try {
            CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
            if (cachePeerEntry == null || stale(cachePeerEntry.date)) {
                //can take seconds if there is a problem
                CachePeer cachePeer = lookupRemoteCachePeer(rmiUrl);
                cachePeerEntry = new CachePeerEntry(cachePeer, new Date());
                //synchronized due to peerUrls being a synchronizedMap
                peerUrls.put(rmiUrl, cachePeerEntry);
            } else {
                cachePeerEntry.date = new Date();
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
                        + e.getMessage());
            }
            unregisterPeer(rmiUrl);
        } catch (NotBoundException e) {
            peerUrls.remove(rmiUrl);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
                        + e.getMessage());
            }
        } catch (Throwable t) {
            LOG.error("Unable to lookup remote cache peer for " + rmiUrl
                    + ". Cause was not due to an IOException or NotBoundException which will occur in normal operation:" +
                    " " + t.getMessage());
        }
    }

    static String extractCacheName(String rmiUrl) {
        return rmiUrl.substring(rmiUrl.lastIndexOf('/') + 1);
    }
    
    /**
     * @return a list of {@link CachePeer} peers, excluding the local peer.
     */
    public final synchronized List listRemoteCachePeers(Ehcache cache) throws CacheException {
        List remoteCachePeers = new ArrayList();
        List staleList = new ArrayList();
        synchronized (peerUrls) {
            for (Iterator iterator = peerUrls.keySet().iterator(); iterator.hasNext();) {
                String rmiUrl = (String) iterator.next();
                String rmiUrlCacheName = extractCacheName(rmiUrl);
                try {
                    if (!rmiUrlCacheName.equals(cache.getName())) {
                        continue;
                    }
                    CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
                    Date date = cachePeerEntry.date;
                    if (!stale(date)) {
                        CachePeer cachePeer = cachePeerEntry.cachePeer;
                        remoteCachePeers.add(cachePeer);
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("rmiUrl " + rmiUrl + " is stale. Either the remote peer is shutdown or the " +
                                    "network connectivity has been interrupted. Will be removed from list of remote cache peers");
                        }
                        staleList.add(rmiUrl);
                    }
                } catch (Exception exception) {
                    LOG.error(exception.getMessage(), exception);
                    throw new CacheException("Unable to list remote cache peers. Error was " + exception.getMessage());
                }
            }
            //Must remove entries after we have finished iterating over them
            for (int i = 0; i < staleList.size(); i++) {
                String rmiUrl = (String) staleList.get(i);
                peerUrls.remove(rmiUrl);
            }
        }
        return remoteCachePeers;
    }


    /**
     * Shutdown the heartbeat
     */
    public final void dispose() {
        heartBeatSender.dispose();
        heartBeatReceiver.dispose();
        
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                LOG.error("Error closing channel. Message was " + e.getMessage(), e);
            }
        }
    }

    /**
     * Time for a cluster to form. This varies considerably, depending on the implementation.
     *
     * @return the time in ms, for a cluster to form
     */
    public long getTimeForClusterToForm() {
        return getStaleTime();
    }

    /**
     * The time after which an unrefreshed peer provider entry is considered stale.
     */
    protected long getStaleTime() {
        return heartBeatSender.getHeartbeatInterval() * 2 + SHORT_DELAY;
    }

    /**
     * Whether the entry should be considered stale.
     * This will depend on the type of RMICacheManagerPeerProvider.
     * This method should be overridden for implementations that go stale based on date
     *
     * @param date the date the entry was created
     * @return true if stale
     */
    protected final boolean stale(Date date) {
        long now = System.currentTimeMillis();
        return date.getTime() < (now - getStaleTime());
    }


    /**
     * Entry containing a looked up CachePeer and date
     */
    protected static final class CachePeerEntry {

        private final CachePeer cachePeer;
        private Date date;

        /**
         * Constructor
         *
         * @param cachePeer the cache peer part of this entry
         * @param date      the date part of this entry
         */
        public CachePeerEntry(CachePeer cachePeer, Date date) {
            this.cachePeer = cachePeer;
            this.date = date;
        }

        /**
         * @return the cache peer part of this entry
         */
        public final CachePeer getCachePeer() {
            return cachePeer;
        }


        /**
         * @return the date part of this entry
         */
        public final Date getDate() {
            return date;
        }

    }

    public UnicastKeepaliveHeartbeatReceiver getHeartBeatReceiver() {
        return heartBeatReceiver;
    }

    public UnicastKeepaliveHeartbeatSender getHeartBeatSender() {
        return heartBeatSender;
    }
}
