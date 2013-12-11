package com.lightstreamer.adapters.LeapMotionDemo;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.lightstreamer.adapters.LeapMotionDemo.room.ChatRoom;
import com.lightstreamer.adapters.metadata.LiteralBasedProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;
import com.lightstreamer.interfaces.metadata.AccessException;
import com.lightstreamer.interfaces.metadata.CreditsException;
import com.lightstreamer.interfaces.metadata.CustomizableItemEvent;
import com.lightstreamer.interfaces.metadata.ItemEvent;
import com.lightstreamer.interfaces.metadata.ItemsException;
import com.lightstreamer.interfaces.metadata.MetadataProvider;
import com.lightstreamer.interfaces.metadata.MetadataProviderException;
import com.lightstreamer.interfaces.metadata.Mode;
import com.lightstreamer.interfaces.metadata.NotificationException;
import com.lightstreamer.interfaces.metadata.SchemaException;
import com.lightstreamer.interfaces.metadata.TableInfo;

public class LeapMotionMetaDataAdapter extends LiteralBasedProvider {

    /**
     * Unique identification of the Adapter Set. It is used to uniquely
     * identify the related Data Adapter instance;
     * see feedMap on LeapMotionDataAdapter.
     */
    private String adapterSetId;
    /**
     * Keeps the client context informations supplied by Lightstreamer on the
     * new session notifications.
     * Session information is needed to pass the IP to logging purpose.
     */
    private ConcurrentHashMap<String,Map<String,String>> sessions = new ConcurrentHashMap<String,Map<String,String>>();
    
    private LeapMotionDataAdapter feed;
    
    private int nextId = 0;
    
    private static final String LOGGER_CAT = "LS_demos_Logger.LeapDemo";
    public static Logger logger;

    @Override
    public void init(Map params, File configDir) throws MetadataProviderException {
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
        
        logger = Logger.getLogger(LOGGER_CAT);
        
        // Read the Adapter Set name, which is supplied by the Server as a parameter
        this.adapterSetId = (String) params.get("adapters_conf.id");
    }
    
    @Override
    public String[] getItems(String user, String session, String group)
            throws ItemsException {
        
        String[] split = super.getItems(user,session,group);
        
        for (int i = 0; i<split.length; i++) {
            if (split[i].indexOf(Constants.USER_SUBSCRIPTION) == 0) {
                if (split[i].indexOf(Constants.SPLIT_CHAR) > -1) {
                    throw new ItemsException("Unexpected "+Constants.SPLIT_CHAR+" charater in item name.");
                } //we may forgive this and later split @ the first occorunce of the SPLIT_CHAR
                
                String nick = split[i].substring(Constants.USER_SUBSCRIPTION.length());
                
                
                String id = null;
                synchronized(sessions) {
                    //we might use the sessionID as userID but that would expose a user's sessionID to
                    //everyone compromising its security
                    
                    Map<String,String> sessionInfo = sessions.get(session);
                    if (sessionInfo == null) {
                        throw new ItemsException("Can't find session");
                    }
                    id = sessionInfo.get(Constants.USER_ID);
                    if (id == null) {
                        id = "u"+(nextId++);
                        sessionInfo.put(Constants.USER_ID, id);
                    }
                }
                
                
                split[i] = Constants.USER_SUBSCRIPTION + id + Constants.SPLIT_CHAR + nick;
            } 
        }
        
        return split;
    }
    
    @Override
    public boolean modeMayBeAllowed(String item, Mode mode) {
        if (item.indexOf(Constants.USER_SUBSCRIPTION) == 0 && mode == Mode.DISTINCT) { 
            return true;
            
        } else if (item.indexOf(Constants.ROOMPOSITION_SUBSCRIPTION) == 0 && mode == Mode.COMMAND) {
            return true;
            
        } else if (item.indexOf(Constants.ROOMCHATLIST_SUBSCRIPTION) == 0  && mode == Mode.COMMAND) {
            return true;
            
        }
        
        return mode == Mode.MERGE;
    }
    
    @Override
    public void notifyUserMessage(String user, String session, String message)
            throws CreditsException, NotificationException {
        
        //be sure we can communicate with the data adapter
        this.loadFeed();
        
        //get the user id of the current user
        String id = null;
        synchronized(sessions) {
            Map<String,String> sessionInfo = sessions.get(session);
            if (sessionInfo == null) {
                throw new CreditsException(-1, "Can't find user id (session missing)");
            }
            id = sessionInfo.get(Constants.USER_ID);
            if (id == null) {
                throw new CreditsException(-2, "Can't find user id (value missing)");
            }
        }
        
        //nick| <-- changing nick
        //status| <-- changing status message
        //enter| <-- enter a room
        //leave| <-- leave a room
        String val;
        if (( val = Constants.getVal(message,Constants.NICK_MESSAGE)) != null) {
            ChatRoom chat = this.feed.getChatFeed();
            chat.changeUserNick(id, val);
        } else if (( val = Constants.getVal(message,Constants.STATUS_MESSAGE)) != null) {
            ChatRoom chat = this.feed.getChatFeed();
            chat.changeUserStatus(id, val, Constants.VOID_STATUS_ID);
        } else if (( val = Constants.getVal(message,Constants.ENTER_ROOM)) != null) {
            ChatRoom chat = this.feed.getChatFeed();
            chat.enterRoom(id,val);
        } else if (( val = Constants.getVal(message,Constants.EXIT_ROOM)) != null) {
            ChatRoom chat = this.feed.getChatFeed();
            chat.leaveRoom(id,val);
        }
    }

    
    private void loadFeed() throws CreditsException {
        if (this.feed == null) {
             try {
                 // Get the LeapMotionDataAdapter instance to bind it with this
                 // Metadata Adapter and send chat messages through it
                 this.feed = LeapMotionDataAdapter.feedMap.get(this.adapterSetId);
             } catch(Throwable t) {
                 // It can happen if the Chat Data Adapter jar was not even
                 // included in the Adapter Set lib directory (the LeapMotion
                 // Data Adapter could not be included in the Adapter Set as well)
                 logger.error("LeapMotionDataAdapter class was not loaded: " + t);
                 throw new CreditsException(0, "No feed available", "No feed available");
             }

             if (this.feed == null) {
                 // The feed is not yet available on the static map, maybe the
                 // LeapMotion Data Adapter was not included in the Adapter Set
                 logger.error("LeapMotionDataAdapter not found");
                 throw new CreditsException(0, "No feed available", "No feed available");
             }
        }
    }
    
    
   
    @Override
    public void notifyNewSession(String user, String session, Map sessionInfo) throws CreditsException, NotificationException {
        // Register the session details on the sessions HashMap.
        sessions.put(session, sessionInfo);
    }
    
    @Override
    public void notifySessionClose(String session) throws NotificationException {
        //we have to remove session informations from the session HashMap
        sessions.remove(session);
    }

}
