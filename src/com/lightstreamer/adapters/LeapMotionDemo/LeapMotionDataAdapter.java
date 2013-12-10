package com.lightstreamer.adapters.LeapMotionDemo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.lightstreamer.adapters.LeapMotionDemo.room.ChatRoom;
import com.lightstreamer.adapters.LeapMotionDemo.room.ChatRoomListener;
import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.FailureException;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.SmartDataProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;

public class LeapMotionDataAdapter implements SmartDataProvider, /*UniverseListener,*/ ChatRoomListener {

    public static final ConcurrentHashMap<String, LeapMotionDataAdapter> feedMap =
            new ConcurrentHashMap<String, LeapMotionDataAdapter>();
   
    public Logger logger;
    //public Logger tracer;
    
    //private Universe universe = new Universe();
    private ChatRoom chat = new ChatRoom();
    private ItemEventListener listener;
    
    @Override
    public void init(Map params, File configDir) throws DataProviderException {
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
        
        logger = Logger.getLogger(Constants.LOGGER_CAT);
        //tracer = Logger.getLogger(TRACER_CAT);
        
        logger.info("Adapter Logger start.");
        //tracer.info("Trace Logger start.");
        
        chat.setListener(this);
      
        
        // Read the Adapter Set name, which is supplied by the Server as a parameter
        String adapterSetId = (String) params.get("adapters_conf.id");
        
        // Put a reference to this instance on a static map
        // to be read by the Metadata Adapter
        feedMap.put(adapterSetId, this);
        
        
        logger.info("LeapMotionAdapter ready");
        
    }
    
    @Override
    public void setListener(ItemEventListener listener) {
        this.listener = listener;
    }


    @Override
    public boolean isSnapshotAvailable(String item)
            throws SubscriptionException {

        if (item.indexOf(Constants.USER_SUBSCRIPTION) == 0) { 
            return false; //currently does not generate any event at all (and never will)
        } else if (item.indexOf(Constants.ROOMPOSITION_SUBSCRIPTION) == 0) {
            return false; //currently does not generate any event at all TODO
        } else if (item.indexOf(Constants.ROOMCHATLIST_SUBSCRIPTION) == 0) {
            return true;
        } else {
            return true; 
        }
    }

    
    @Override
    public synchronized void subscribe(String item, Object handle, boolean needsIterator)
            throws SubscriptionException, FailureException {

        if (item.indexOf(Constants.USER_SUBSCRIPTION) == 0) { 
            //DISTINCT used only to signal presence
            logger.debug("User subscription: " + item);
            
            //item comes from client as user_nick, is modified by metadata as user_id|nick
            String[] ids = item.substring(Constants.USER_SUBSCRIPTION.length()).split(Constants.SPLIT_CHAR);
            if (ids.length != 2) {
                throw new SubscriptionException("Unexpected user item: review getItems metadata implementation");
            }
            
            //user is created on subscription and destroyed on unsubscription
            chat.startUserMessageListen(ids[0],handle);
            chat.changeUserNick(ids[0],ids[1]);
            
        } else if (item.indexOf(Constants.ROOMPOSITION_SUBSCRIPTION) == 0) {
            //COMMAND contains list of users and object positions
            logger.debug("Position subscription: " + item);
            
            String roomId = item.substring(Constants.ROOMPOSITION_SUBSCRIPTION.length());
            
            // TODO
            
        } else if (item.indexOf(Constants.ROOMCHATLIST_SUBSCRIPTION) == 0) {
            //COMMAND contains user statuses and user nicks
            logger.debug("Room list subscription: " + item);
            
            String roomId = item.substring(Constants.ROOMCHATLIST_SUBSCRIPTION.length());
            chat.startRoomListen(roomId,handle);// will add the room if non-existent (room may exist if a user entered it even if no one is listening to it)

        } else {
            //MERGE subscription for user status and nick 
            logger.debug("User status subscription: " + item);
            chat.startUserStatusListen(item,handle);
        }
    }

    @Override
    public synchronized void unsubscribe(String item) throws SubscriptionException,
            FailureException {
        
        if (item.indexOf(Constants.USER_SUBSCRIPTION) == 0) {
            logger.debug("User unsubscription: " + item);
            
            String[] ids = item.substring(Constants.USER_SUBSCRIPTION.length()).split(Constants.SPLIT_CHAR);
            if (ids.length != 2) {
                return;
            }
            chat.stopUserMessageListen(ids[0]);
            chat.removeUser(ids[0]);
            
        } else if (item.indexOf(Constants.ROOMPOSITION_SUBSCRIPTION) == 0) {
            logger.debug("Position unsubscription: " + item);
            
            String roomId = item.substring(Constants.ROOMPOSITION_SUBSCRIPTION.length());
            
            // TODO create 3d room
            
        } else if (item.indexOf(Constants.ROOMCHATLIST_SUBSCRIPTION) == 0) {
            logger.debug("Room list unsubscription: " + item);
            
            String roomId = item.substring(Constants.ROOMCHATLIST_SUBSCRIPTION.length());
            chat.stopRoomListen(roomId);
        } else {
            logger.debug("User status unsubscription: " + item);
            
            chat.stopUserStatusListen(item);
        }
    }
    
    //user related events sequentiality is ensured by the chat class  

        //room-enter-exit handling
    @Override
    public void onUserEnter(String id, String room, Object roomStatusHandle, boolean realTimeEvent) {
        logger.debug(id + " enters " + room);
        
        HashMap<String, String> update = new HashMap<String, String>();
        update.put(SmartDataProvider.KEY_FIELD, id);
        update.put(SmartDataProvider.COMMAND_FIELD, SmartDataProvider.ADD_COMMAND);
              
        this.listener.smartUpdate(roomStatusHandle, update, !realTimeEvent);
        
    
        // TODO add user to 3d world
    }
    
    @Override
    public void onRoomListComplete(String id, Object roomStatusHandle) {
        logger.debug(id + " filled");
        
        this.listener.smartEndOfSnapshot(roomStatusHandle);
    }

    @Override
    public void onUserExit(String id, String room, Object roomStatusHandle) {
        logger.debug(id + " exits " + room);
        
        HashMap<String, String> update = new HashMap<String, String>();
        update.put(SmartDataProvider.KEY_FIELD, id);
        update.put(SmartDataProvider.COMMAND_FIELD, SmartDataProvider.DELETE_COMMAND);
        
        this.listener.smartUpdate(roomStatusHandle, update, false);
        
        
     // TODO remove user from 3D world
    }

        //user nick-status changes 
    
    @Override
    public void onUserStatusChange(String id, String nick, String statusId, String status, Object userStatusHandle, boolean realTimeEvent) {
        logger.debug(id + " has new status/nick");
        
        HashMap<String, String> update = new HashMap<String, String>();
        update.put("nick", nick);
        update.put("status", status);
        update.put("statusId", statusId);
        
        this.listener.smartUpdate(userStatusHandle, update, !realTimeEvent);
        
    }

    @Override
    public void onUserMessage(String id, String message, String room, Object roomHandle, boolean realTimeEvent) {
      //not used - not implemented
        logger.debug(id + " sent a message to " + room);
    }

    @Override
    public void onPrivateMessage(String fromId, String toId, String message, Object userHandle) { //always realtime (can't send messages to disconnected users)
      //not used - not implemented
        logger.debug(toId + " got a private message from " + fromId);
    }
    
    @Override
    public void onNewUser(String id) {
        //do nothing
        logger.debug(id + " is ready");
    }

    @Override
    public void onUserDeleted(String id) {
        //do nothing
        logger.debug(id + " is gone");
    }
    
    @Override
    public void subscribe(String arg0, boolean arg1)
            throws SubscriptionException, FailureException {
        // unused
        logger.error("Unexpected call");
        throw new SubscriptionException("Unexpected call");
    }

}
