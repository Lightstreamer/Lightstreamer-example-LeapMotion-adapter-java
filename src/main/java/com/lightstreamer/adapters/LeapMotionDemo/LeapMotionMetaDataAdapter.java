/*
  Copyright (c) Lightstreamer Srl

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package com.lightstreamer.adapters.LeapMotionDemo;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.metadata.CreditsException;
import com.lightstreamer.adapter.metadata.LoginInfo;
import com.lightstreamer.adapter.metadata.MetadataAdapter;
import com.lightstreamer.adapter.metadata.SessionListener;
import com.lightstreamer.adapter.metadata.SessionRequestHandler;
import com.lightstreamer.adapter.metadata.SubscriptionHandler;
import com.lightstreamer.adapter.metadata.SubscriptionInfo;
import com.lightstreamer.adapters.LeapMotionDemo.engine3D.Universe;
import com.lightstreamer.adapters.LeapMotionDemo.room.ChatRoom;

public class LeapMotionMetaDataAdapter implements MetadataAdapter {

    /**
     * Maps each active user ID to the session that owns it,
     * to ensure uniqueness across sessions.
     */
    private final ConcurrentHashMap<String,String> ids = new ConcurrentHashMap<String,String>();
    
    private volatile LeapMotionDataAdapter feed;
    private volatile String dataAdapterName;

    private Logger logger;

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {
        /*
        String logConfig = (String) params.get("log_config");
        if (logConfig != null) {
            File logConfigFile = new File(configDir, logConfig);
            String logRefresh = (String) params.get("log_config_refresh_seconds");
            if (logRefresh != null) {
                DOMConfigurator.configureAndWatch(logConfigFile.getAbsolutePath(), Integer.parseInt(logRefresh) * 1000);
            } else {
                DOMConfigurator.configure(logConfigFile.getAbsolutePath());
            }
        } //else the bridge to logback is expected
        */
        
        logger = LogManager.getLogger(Constants.LOGGER_CAT);
    }

    @Override
    public void onDataAdapterAvailable(String name, Object adapter) {
        if (!(adapter instanceof LeapMotionDataAdapter)) {
            return;
        }
        if (feed != null) {
            logger.warn("A LeapMotionDataAdapter is already bound; ignoring " + name);
            return;
        }
        feed = (LeapMotionDataAdapter) adapter;
        dataAdapterName = name;
        logger.debug("LeapMotionDataAdapter bound: " + name);
    }

    @Override
    public void onDataAdapterUnavailable(String name) {
        if (!name.equals(dataAdapterName)) {
            return;
        }
        feed = null;
        dataAdapterName = null;
        logger.debug("LeapMotionDataAdapter unbound: " + name);
    }
    
    @Override
    public CompletionStage<SessionRequestHandler> loginAsync(LoginInfo loginInfo) {
        //////////////////////////////////////////////////////////////////////////
        // WARNING: No authentication logic is implemented; any login is accepted.
        // This is suitable for demos only and MUST NOT be used in production.
        //////////////////////////////////////////////////////////////////////////
        Map<String, String> sessionInfo = loginInfo.getClientContext();
        return CompletableFuture.completedFuture(new SessionRequestHandler() {

            private String sessionId;
            private String userId;

            @Override
            public void onStart(SessionListener listener) {
                sessionId = listener.getSessionId();
                logger.info("New session available " + sessionId);
            }

            @Override
            public Double getAllowedMaxBandwidth() {
                return null;
            }

            @Override
            public CompletionStage<SubscriptionHandler> newSubscription(SubscriptionInfo info) throws CreditsException {
                for (int i = 0; i < info.getItemRefs().size(); i++) {
                    String item = info.getItemRefs().get(i);
                    String val;
                    if (( val = Constants.getVal(item,Constants.USER_SUBSCRIPTION)) != null) {
                        synchronized(ids) {
                            if (userId != null) {
                                //currently permit only one id per session 
                                if (!val.equals(userId)) {
                                    logger.warn("Session alredy owns an ID: " + val);
                                    throw new CreditsException(-1, "Session alredy owns an ID: " + val);
                                }
                                
                            } else {
                                if (ids.containsKey(val)) {
                                    logger.debug("Id already taken: " + val);
                                    throw new CreditsException(-1, "Id already taken, try again");
                                } 
                                userId = val;
                            }
                            ids.put(val, sessionId);
                            if (feed == null) {
                                throw new CreditsException(0, "No feed available");
                            }
                            ChatRoom chat = feed.getChatFeed();
                            chat.startUser(val);
                        }
                    }
                }
                
                return CompletableFuture.completedFuture(new SubscriptionHandler(info));
            }

            @Override
            public CompletionStage<String> userMessageAsync(String message) throws CreditsException {
                // we won't introduce blocking operations, hence we can proceed inline

                //be sure we can communicate with the data adapter
                if (feed == null) {
                    throw new CreditsException(0, "No feed available");
                }
                
                //get the user id of the current user
                if (userId == null) {
                    logger.warn("Can't find user id");
                    throw new CreditsException(-2, "Can't find user id (value missing)");
                }
                String id = userId;
                String IP = sessionInfo.get("REMOTE_IP");
                
                //nick| <-- changing nick
                //status| <-- changing status message
                //enter| <-- enter a room
                //leave| <-- leave a room
                //grab| <-- grab the cube
                //release| <-- release the cube
                //move| <-- move the cube
                
                String val;
                if (( val = Constants.getVal(message,Constants.NICK_MESSAGE)) != null) {
                    logger.debug("new nick message from "+id+" received: " + message);
                    
                    ChatRoom chat = feed.getChatFeed();
                    chat.changeUserNick(id, val, IP);
                    
                } else if (( val = Constants.getVal(message,Constants.STATUS_MESSAGE)) != null) {
                    logger.debug("new status message from "+id+" received: " + message);
                    
                    ChatRoom chat = feed.getChatFeed();
                    chat.changeUserStatus(id, val, Constants.VOID_STATUS_ID, IP);
                    
                } else if (( val = Constants.getVal(message,Constants.ENTER_ROOM)) != null) {
                    logger.debug("new enter-room message from "+id+" received: " + message);
                    
                    ChatRoom chat = feed.getChatFeed();
                    chat.enterRoom(id,val);
                    
                } else if (( val = Constants.getVal(message,Constants.EXIT_ROOM)) != null) {
                    logger.debug("new exit-room message from "+id+" received: " + message);
                    
                    ChatRoom chat = feed.getChatFeed();
                    chat.leaveRoom(id,val);
                    
                } else if (( val = Constants.getVal(message,Constants.GRAB_MESSAGE)) != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("new grab message from "+id+" received: " + message);
                    }
                    
                    Universe universe = feed.getUniverse();
                    universe.block(id,val);
                    
                } else if (( val = Constants.getVal(message,Constants.RELEASE_MESSAGE)) != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("new release message from "+id+" received: " + message);
                    }
                    
                    Universe universe = feed.getUniverse();
                    String[] values = val.split(Constants.SPLIT_CHAR_REG);
                    double[] dobuleValues = getDoubles(values);
                    universe.release(id,values[0],dobuleValues[0],dobuleValues[1],dobuleValues[2]);
                    
                } else if (( val = Constants.getVal(message,Constants.MOVE_MESSAGE)) != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("new move message from "+id+" received: " + message);
                    }
                    
                    Universe universe = feed.getUniverse();
                    String[] values = val.split(Constants.SPLIT_CHAR_REG);
                    double[] dobuleValues = getDoubles(values);
                    universe.move(id,values[0],dobuleValues[0],dobuleValues[1],dobuleValues[2]);
                    
                } else {
                    logger.warn("unexpected message from "+id+" received: " + message);
                    throw new CreditsException(-3, "Unexpected message");
                }

                return CompletableFuture.completedFuture(null);
            }

            private static double[] getDoubles(String[] values) throws CreditsException {
                if (values.length != 4) {
                    throw new CreditsException(-4, "wrongly formatted message");
                }
                double[] res = new double[3];
                try {
                    res[0] = Double.parseDouble(values[1]);
                    res[1] = Double.parseDouble(values[2]);
                    res[2] = Double.parseDouble(values[3]);
                } catch(NumberFormatException nf) {
                    throw new CreditsException(-5, "wrongly formatted number");
                }
                return res;
            }

            @Override
            public void onClose() {
                //we have to clean up session information
                logger.info("Clearing session " + sessionId);
                String id = userId;
                if (id != null) {
                    synchronized(ids) {
                        ids.remove(id);
                    }
                    
                    if (feed == null) {
                        logger.error("Unexpected: feed not available");
                        return;
                    }
                    
                    ChatRoom chat = feed.getChatFeed();
                    chat.leaveAllRooms(id);
                    chat.stopUser(id);
                }
            }
        });
    }

    @Override
    public void shutdown() {
    }
}