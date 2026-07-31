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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.common.Mode;
import com.lightstreamer.adapter.data.DataAdapter;
import com.lightstreamer.adapter.data.ItemListener;
import com.lightstreamer.adapter.data.ItemManager;
import com.lightstreamer.adapter.data.SubscriptionException;
import com.lightstreamer.adapters.LeapMotionDemo.engine3D.Universe;
import com.lightstreamer.adapters.LeapMotionDemo.engine3D.UniverseListener;
import com.lightstreamer.adapters.LeapMotionDemo.room.ChatRoom;
import com.lightstreamer.adapters.LeapMotionDemo.room.ChatRoomListener;
import com.lightstreamer.adapters.LeapMotionDemo.room.User;

public class LeapMotionDataAdapter implements DataAdapter, UniverseListener, ChatRoomListener {

    private Logger logger;
    
    private final Universe universe = new Universe(this);
    private final ChatRoom chat = new ChatRoom(this);
    
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
        
        logger.info("Adapter Logger start.");
        
        logger.info("LeapMotionDataAdapter ready");
    }

    ChatRoom getChatFeed() {
        return this.chat;
    }
    
    Universe getUniverse() {
        return this.universe;
    }

    @Override
    public CompletionStage<ItemManager> getItemManager(String itemName) {
        String val;
        if (( val = Constants.getVal(itemName, Constants.USER_SUBSCRIPTION)) != null) {
            return CompletableFuture.completedFuture(new UserMessageItemManager(val, itemName));
        } else if (itemName.indexOf(Constants.ROOMPOSITION_SUBSCRIPTION) == 0) {
            String roomId = itemName.substring(Constants.ROOMPOSITION_SUBSCRIPTION.length());
            return CompletableFuture.completedFuture(new RoomPositionItemManager(roomId, itemName));
        } else if (itemName.indexOf(Constants.ROOMCHATLIST_SUBSCRIPTION) == 0) {
            String roomId = itemName.substring(Constants.ROOMCHATLIST_SUBSCRIPTION.length());
            return CompletableFuture.completedFuture(new RoomChatListItemManager(roomId, itemName));
        } else {
            return CompletableFuture.completedFuture(new UserStatusItemManager(itemName));
        }
    }

    /**
     * DISTINCT item for user message presence signals.
     */
    private class UserMessageItemManager implements ItemManager {
        private final String userId;
        private final String item;

        UserMessageItemManager(String userId, String item) {
            this.userId = userId;
            this.item = item;
        }

        @Override
        public Mode getSupportedMode() {
            return Mode.DISTINCT;
        }

        @Override
        public Integer getDistinctSnapshotLength() {
            return 30;
        }

        @Override
        public boolean isSnapshotAvailable() {
            return false; //currently does not generate any event at all (and never will)
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) {
            logger.debug("User subscription: " + item);
            //user is created on subscription and destroyed on unsubscription
            chat.startUserMessageListen(userId, listener);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> unsubscribe() {
            logger.debug("User unsubscription: " + item);
            chat.stopUserMessageListen(userId);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * COMMAND item for the list of users and object positions in a world.
     */
    private class RoomPositionItemManager implements ItemManager {
        private final String roomId;
        private final String item;

        RoomPositionItemManager(String roomId, String item) {
            this.roomId = roomId;
            this.item = item;
        }

        @Override
        public Mode getSupportedMode() {
            return Mode.COMMAND;
        }

        @Override
        public boolean isSnapshotAvailable() {
            return true;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) {
            logger.debug("Position subscription: " + item);
            universe.startWatchingWorld(roomId, listener);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> unsubscribe() {
            logger.debug("Position unsubscription: " + item);
            universe.stopWatchingWorld(roomId);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * COMMAND item for the list of users in a chat room.
     */
    private class RoomChatListItemManager implements ItemManager {
        private final String roomId;
        private final String item;

        RoomChatListItemManager(String roomId, String item) {
            this.roomId = roomId;
            this.item = item;
        }

        @Override
        public Mode getSupportedMode() {
            return Mode.COMMAND;
        }

        @Override
        public boolean isSnapshotAvailable() {
            return true;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) {
            logger.debug("Room list subscription: " + item);
            chat.startRoomListen(roomId, listener);// will add the room if non-existent (room may exist if a user entered it even if no one is listening to it)
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> unsubscribe() {
            logger.debug("Room list unsubscription: " + item);
            chat.stopRoomListen(roomId);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * MERGE item for user status, nick, commands, and forced positions.
     */
    private class UserStatusItemManager implements ItemManager {
        private final String item;

        UserStatusItemManager(String item) {
            this.item = item;
        }

        @Override
        public Mode getSupportedMode() {
            return Mode.MERGE;
        }

        @Override
        public boolean isSnapshotAvailable() {
            return true;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) {
            logger.debug("User status subscription: " + item);
            chat.startUserStatusListen(item, listener);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> unsubscribe() {
            logger.debug("User status unsubscription: " + item);
            chat.stopUserStatusListen(item);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    //user related events sequentiality is ensured by the chat class; are only generated if there is the associated handle  

    @Override
    public void onUserEnter(User user, String room, ItemListener roomStatusHandle, boolean realTimeEvent) {
        String id = user.getId();
        logger.debug(id + " enters " + room);
        
        HashMap<String, String> update = new HashMap<String, String>();
        update.put(ItemListener.KEY_FIELD, id);
        update.put(ItemListener.COMMAND_FIELD, ItemListener.ADD_COMMAND);
              
        if (realTimeEvent) {
            roomStatusHandle.update(update);
        } else {
            roomStatusHandle.snapshot(update, null);
        }
        
        universe.addPlayerToWorld(id,room);
        
    }
    
    @Override
    public void onRoomListComplete(String id, ItemListener roomStatusHandle) {
        logger.debug(id + " filled");
        
        roomStatusHandle.endOfSnapshot();
    }

    @Override
    public void onUserExit(User user, String room, ItemListener roomStatusHandle) {
        String id = user.getId();
        logger.debug(id + " exits " + room);
        
        HashMap<String, String> update = new HashMap<String, String>();
        update.put(ItemListener.KEY_FIELD, id);
        update.put(ItemListener.COMMAND_FIELD, ItemListener.DELETE_COMMAND);
        
        roomStatusHandle.update(update);
        
        universe.removePlayerFromWorld(id,room);
    }

    
    @Override
    public void onUserStatusChange(User user, String nick, String statusId, String status, Map<String,String> extra, ItemListener userStatusHandle, boolean realTimeEvent) {
        String id = user.getId();
        logger.debug(id + " has new status/nick");
        
        if (extra == null) {
            extra =  new HashMap<String, String>();
        }
       
        if (nick != null) {
            extra.put("nick", nick);
        }
        if (status != null) {
            extra.put("status", status);
            extra.put("statusId", statusId);
        }
        
        if (realTimeEvent) {
            userStatusHandle.update(extra);
        } else {
            userStatusHandle.snapshot(extra, null);
        }
        
        //note: in case of a !realtime event, if we assign an initial movement to the players we'll have to check if the player for this user already exists and grab its forces
        
    }

    @Override
    public void onUserMessage(String id, String message, String room, ItemListener roomHandle, boolean realTimeEvent) {
      //not used
        logger.debug(id + " sent a message to " + room);
    }

    @Override
    public void onPrivateMessage(String fromId, String toId, String message, ItemListener userHandle) { //always realtime (can't send messages to disconnected users)
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
    
    // universe events are generated even if there is no handle
    
    @Override
    public void onWorldComplete(String worldId, ItemListener worldHandle) {
        logger.debug("World " + worldId + " flushed");
        
        if (worldHandle != null) {
            worldHandle.endOfSnapshot();
        }
    }

    @Override
    public void onPlayerCreated(String id, String worldId, ItemListener worldHandle, HashMap<String,String> currentPosition,  HashMap<String,String> currentImpulses, boolean realTimeEvent) {
        logger.debug(id + " enters world " + worldId);
        
        if (worldHandle != null) {
            currentPosition.put(ItemListener.KEY_FIELD, id);
            currentPosition.put(ItemListener.COMMAND_FIELD, ItemListener.ADD_COMMAND); 
            if (realTimeEvent) {
                worldHandle.update(currentPosition);
            } else {
                worldHandle.snapshot(currentPosition, null);
            }
        }
        
        ItemListener userHandle = chat.getUserStatusHandle(id);
        if (userHandle != null) {
            userHandle.update(currentImpulses);
        }
    }
    
    @Override
    public void onPlayerMoved(String id, String worldId, ItemListener worldHandle,
            HashMap<String, String> currentPosition, boolean forced) {
        
        if (logger.isTraceEnabled()) {
            logger.trace(id + " in world " + worldId + " moves");
        }
        
        if (!forced) {
            if (worldHandle != null) {
                currentPosition.put(ItemListener.KEY_FIELD, id);
                currentPosition.put(ItemListener.COMMAND_FIELD, ItemListener.UPDATE_COMMAND);
                worldHandle.update(currentPosition);
            }
        } else {
            ItemListener userHandle = chat.getUserStatusHandle(id);
            if (userHandle != null) {
                userHandle.update(currentPosition);
            }
        }
    }

    @Override
    public void onPlayerActed(String id, String worldId, ItemListener worldHandle,
            HashMap<String, String> currentImpulses) {
        
        if (logger.isTraceEnabled()) {
            logger.trace(id + " in world " + worldId + " acts"); //block/release actions
        }
        
        ItemListener userHandle = chat.getUserStatusHandle(id);
        if (userHandle != null) {
            userHandle.update(currentImpulses);
        }
    }

    @Override
    public void onPlayerDisposed(String id, String worldId, ItemListener worldHandle) {
        logger.debug(id + " exits world " + worldId);
        if (worldHandle != null) {
            HashMap<String, String> update = new HashMap<String, String>();
            update.put(ItemListener.KEY_FIELD, id);
            update.put(ItemListener.COMMAND_FIELD, ItemListener.DELETE_COMMAND);
            
            worldHandle.update(update);
        }
    }

    @Override
    public void shutdown() {
    }
}