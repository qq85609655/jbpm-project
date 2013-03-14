/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.androidpn.server.xmpp.session;

import org.androidpn.server.util.Config;
import org.androidpn.server.xmpp.XmppServer;
import org.androidpn.server.xmpp.net.Connection;
import org.androidpn.server.xmpp.net.ConnectionCloseListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xmpp.packet.JID;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 
 * This class manages the sessions connected to the server.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class SessionManager {

    private static final Log log = LogFactory.getLog(SessionManager.class);

    private static final String RESOURCE_NAME = "AndroidpnClient";

    private static SessionManager instance;

    private String serverName;

    private Map<String, ClientSession> preAuthSessions = new ConcurrentHashMap<String, ClientSession>();

    private Map<String, ClientSession> clientSessions = new ConcurrentHashMap<String, ClientSession>();

    private final AtomicInteger connectionsCounter = new AtomicInteger(0);

    private ClientSessionListener clientSessionListener = new ClientSessionListener();

    private SessionManager() {
        serverName = XmppServer.getInstance().getServerName();
        //TODO Added by ken
        Thread thread = new Thread(new ContainerBackgroundProcessor());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Returns the singleton instance of SessionManager.
     * 
     * @return the instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                instance = new SessionManager();
            }
        }
        return instance;
    }

    /**
     * Creates a new ClientSession and returns it.
     *  
     * @param conn the connection
     * @return a newly created session
     */
    public ClientSession createClientSession(Connection conn) {
        if (serverName == null) {
            throw new IllegalStateException("Server not initialized");
        }

        Random random = new Random();
        String streamId = Integer.toHexString(random.nextInt());

        ClientSession session = new ClientSession(serverName, conn, streamId);
        conn.init(session);
        conn.registerCloseListener(clientSessionListener);

        // Add to pre-authenticated sessions
        preAuthSessions.put(session.getAddress().getResource(), session);

        // Increment the counter of user sessions
        connectionsCounter.incrementAndGet();

        log.debug("ClientSession created.");
        return session;
    }

    /**
     * Adds a new session that has been authenticated. 
     *  
     * @param session the session
     */
    public void addSession(ClientSession session) {
        preAuthSessions.remove(session.getStreamID().toString());
        clientSessions.put(session.getAddress().toString(), session);
    }

    /**
     * Returns the session associated with the username.
     * 
     * @param username the username of the client address
     * @return the session associated with the username
     */
    public ClientSession getSession(String username) {
        // return getSession(new JID(username, serverName, null, true));
        return getSession(new JID(username, serverName, RESOURCE_NAME, true));
    }

    /**
     * Returns the session associated with the JID.
     * 
     * @param from the client address
     * @return the session associated with the JID
     */
    public ClientSession getSession(JID from) {
        if (from == null || serverName == null
                || !serverName.equals(from.getDomain())) {
            return null;
        }
        // Check pre-authenticated sessions
        if (from.getResource() != null) {
            ClientSession session = preAuthSessions.get(from.getResource());
            if (session != null) {
                return session;
            }
        }
        if (from.getResource() == null || from.getNode() == null) {
            return null;
        }
        return clientSessions.get(from.toString());
    }

    /**
     * Returns a list that contains all authenticated client sessions.
     * 
     * @return a list that contains all client sessions
     */
    public Collection<ClientSession> getSessions() {
        return clientSessions.values();
    }

    /**
     * Removes a client session.
     * 
     * @param session the session to be removed
     * @return true if the session was successfully removed 
     */
    public boolean removeSession(ClientSession session) {
        if (session == null || serverName == null) {
            return false;
        }
        JID fullJID = session.getAddress();

        // Remove the session from list
        boolean clientRemoved = clientSessions.remove(fullJID.toString()) != null;
        boolean preAuthRemoved = (preAuthSessions.remove(fullJID.getResource()) != null);

        // Decrement the counter of user sessions
        if (clientRemoved || preAuthRemoved) {
            connectionsCounter.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Closes the all sessions. 
     */
    public void closeAllSessions() {
        try {
            // Send the close stream header to all connections
            Set<ClientSession> sessions = new HashSet<ClientSession>();
            sessions.addAll(preAuthSessions.values());
            sessions.addAll(clientSessions.values());

            for (ClientSession session : sessions) {
                try {
                    session.getConnection().systemShutdown();
                } catch (Throwable t) {
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * A listner to handle a session that has been closed.
     */
    private class ClientSessionListener implements ConnectionCloseListener {

        public void onConnectionClose(Object handback) {
            try {
                ClientSession session = (ClientSession) handback;
                removeSession(session);
            } catch (Exception e) {
                log.error("Could not close socket", e);
            }
        }
    }
    
    //TODO Added by ken
    private class ContainerBackgroundProcessor implements Runnable {

        private final Log log = LogFactory.getLog(getClass());
        private long checkTimeoutInterval;
        private long maxInactiveInterval;
        
        public ContainerBackgroundProcessor() {
            checkTimeoutInterval = Config.getLong("xmpp.session.checkTimeoutInterval", 10 * 1000);
            maxInactiveInterval = Config.getLong("xmpp.session.maxInactiveInterval", -1);
            log.debug("maxInactiveInterval:" + maxInactiveInterval);
            log.info("maxInactiveInterval:" + maxInactiveInterval);
        }

        @SuppressWarnings("static-access")
        public void run() {
            long sleep = checkTimeoutInterval;
            while (true) {
                processExpires();
                try {
                    Thread.currentThread().sleep(sleep);
                } catch (InterruptedException e) {
                    log.debug(e);
                }
            }
        }
        
        public void processExpires() {
            long timeNow = System.currentTimeMillis();
            int expireHere = 0;
            int size = getSessions().size();

            if (log.isDebugEnabled())
                log.debug("Start expire sessions at " + timeNow + " sessioncount " + size);
            log.info("Start expire sessions at " + timeNow + " sessioncount " + size);
            for (ClientSession session : getSessions()) {
                if (isValid(session)) {
                    expireHere++;
                    session.close();
                }
            }
            long timeEnd = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("End expire sessions processingTime " + (timeEnd - timeNow)
                        + " expired sessions: " + expireHere);
            log.info("End expire sessions processingTime " + (timeEnd - timeNow)
                    + " expired sessions: " + expireHere);
        }
        
        public boolean isValid(ClientSession clientSessions) {  
            if (clientSessions == null) {  
                return false;  
            }
            if (maxInactiveInterval >= 0) {
                long timeNow = System.currentTimeMillis();  
                int timeIdle = (int) ((timeNow - clientSessions.getLastActiveTimeMillis()) / 1000L); 
                return timeIdle >= maxInactiveInterval / 1000L;
            }
          
            return false;  
        }  

    }


}
