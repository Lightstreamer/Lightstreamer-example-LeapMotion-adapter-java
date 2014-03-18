/*
Copyright 2014 Weswit s.r.l.

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
package com.lightstreamer.adapters.LeapMotionDemo.room;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.lightstreamer.adapters.LeapMotionDemo.Constants;



public class ChatRoom {

    private ChatRoomListener listener;
    
    static final boolean ENTER = true;
    static final boolean EXIT = false;
    
    static final boolean REALTIME = true;
    static final boolean SNAPSHOT = false;
    
    private Logger logger = Logger.getLogger(Constants.CHAT_CAT);
    
    
    private HashMap<String,User> users = new HashMap<String,User>();
    private HashMap<String,Room> rooms = new HashMap<String,Room>();
    
    private Executor executor =  Executors.newSingleThreadExecutor();
    
    public ChatRoom(ChatRoomListener listener) {
        this.listener = listener;
    }
    
    // USER INTERNAL HANDLING
    
    private User getUserForced(String id) {
        synchronized(users) {
            User user;
            if (!users.containsKey(id)) {
                user = this.addUser(id);
            } else {
                user = users.get(id);
            }
            return user;
        }
    }
    
    private User addUser(final String id) {
        synchronized(users) {
            logger.info("Creating new user " + id);
            
            User user = new User(this, id);
            users.put(id, user);
            
            executor.execute(new Runnable() {
                public void run() {
                    listener.onNewUser(id);
                }
            });
            
            return user;
        }
    }
    
    private void removeUser(final String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            logger.info("Destroying user " + id);
            
            User user = users.remove(id);
            
            this.removeUserFromRooms(user);
            
            executor.execute(new Runnable() {
                public void run() {
                    listener.onUserDeleted(id);
                }
            });
        }
    }
    
    //synchronized(users)
    private void removeUserFromRooms(User user) {
        synchronized(rooms) {
            Iterator<Room> userRooms = user.getRooms();
            while(userRooms.hasNext()) {
                Room room = userRooms.next();
                user.leaveRoom(room);
            }
        }
    }
    
    private void checkUser(User user) {
        if (!user.isListened() && !user.isActive()) {
            this.removeUser(user.getId());
        }
    }
    
    // USER LIFE-CYCLE
        // IN
    
    public void startUser(String id) {
        synchronized(users) {
            logger.trace("User startup " + id);
            
            User user = this.getUserForced(id);
            user.setActive(true);
        }
    }
    
    public void startUserMessageListen(String id, Object handle) {
        synchronized(users) {
            logger.trace("User private messages startup " + id);
            
            User user = this.getUserForced(id);
            user.setHandle(handle);
        }
    }
    
    public void startUserStatusListen(String id, Object userStatusHandle) {
        synchronized(users) {
            logger.trace("User status messages startup " + id);
            
            User user = this.getUserForced(id);
            user.setStatusHandle(userStatusHandle);
            
            Map<String,String> extra = new HashMap<String,String>();
            extra.putAll(user.getExtraProps());
            this.sendUserStatusEvent(user, user.getNick(), user.getStatusId(), user.getStatus(), extra, userStatusHandle, SNAPSHOT);
        }
    }
    
    // USER LIFE-CYCLE
        // OUT
    
    public void stopUser(String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            logger.trace("User stop " + id);
            
            User user = users.get(id);
            user.setActive(false);
            
            this.checkUser(user);
        }
    }
    
    public void stopUserMessageListen(String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            logger.trace("User private messages stop " + id);
            
            User user = users.get(id);
            user.setHandle(null);
            
            this.checkUser(user);
        }
    }
    
    public void stopUserStatusListen(String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            logger.trace("User status messages stop " + id);
            
            User user = users.get(id);
            user.setStatusHandle(null);
            
            this.checkUser(user);
        }
    }

    // USER ACTIONS 
        // PROPERTIES
    
    public void changeUserNick(String id, String nick) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            logger.debug("User new nick " + id + ": " + nick);
            
            User user = users.get(id);
            user.setNick(nick);
        }
    }
    
    public void changeUserStatus(String id, String status, String statusId) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            logger.debug("User new status " + id + ": " + status);
            
            User user = users.get(id);
            user.setStatus(status,statusId);
        }
    }
    
    // USER ACTIONS 
        // ROOMS
    
    public void enterRoom(String id, String roomId) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            logger.trace("User entering room " + id + ": " + roomId);
            
            User user = users.get(id);
            Room room = this.getRoomForced(roomId);
            user.enterRoom(room);
            
        }
    }
    
    public void leaveRoom(String id, String roomId) {
        synchronized(users) {
            synchronized(rooms) {
                if (!users.containsKey(id) || !rooms.containsKey(roomId)) {
                    return;
                }
                logger.trace("User leaving room " + id + ": " + roomId);
                
                User user = users.get(id);
                Room room = rooms.get(roomId);
                user.leaveRoom(room);
                
                if (room.isEmpty() && !room.isListened()) {
                    rooms.remove(roomId);
                }
            }
        }
    }
    
    public void leaveAllRooms(String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            User user = this.getUserForced(id);
            
            this.removeUserFromRooms(user);
            
            if (!user.isListened()) {
                this.removeUser(id);
            }
            
        }
    }
    
    // USER ACTIONS 
        // CHAT
    
    public void broadcastMessage(String id, String roomId, String message) {
        synchronized(users) {
            synchronized(rooms) {
                if (!users.containsKey(id) || !rooms.containsKey(roomId)) {
                    return;
                }
                logger.debug("User " + id + " message to room " + roomId + ": " + message);
                
                User user = users.get(id);
                Room room = rooms.get(roomId);
                room.broadcastMessage(user,message);
            }
        }
        
    }
    
    // USER EVENTS    
        
    //synchronized(users) {
    void sendUserStatusEvent(final User user, final String nick, final String statusId, final String status, final Map<String,String> extra, final Object userStatusHandle, final boolean realTimeEvent) {
        executor.execute(new Runnable() {
            public void run() {
                listener.onUserStatusChange(user, nick, statusId, status, extra, userStatusHandle, realTimeEvent);
            }
        });
    }
    
    // ROOM INTERNAL HANDLING
    
    private Room getRoomForced(String roomId) {
        synchronized(rooms) {
            Room room = null;
            if (!rooms.containsKey(roomId)) {
                room = this.addRoom(roomId);
            } else {
                room = rooms.get(roomId);
            }
            return room;
        }
    }
    
    private Room addRoom(String roomId) {
        synchronized(rooms) {
            logger.info("Creating new room " + roomId);
            
            Room room = new Room(this, roomId);
            rooms.put(roomId, room);
            return room;
        }
    }
    
    private void removeRoom(String roomId) {
        synchronized(rooms) {
            if (!rooms.containsKey(roomId)) {
                return;
            }
            logger.info("Destroying room " + roomId);
            
            rooms.remove(roomId);
        }
    }
    
    private void checkRoom(Room room) {
        if (room.isEmpty() && !room.isListened()) {
            this.removeRoom(room.getId());
        }
    }
    
    // ROOM LIFECYCLE
        // IN
    
    public void startRoomListen(final String roomId, final Object roomStatusHandle) {
        
        synchronized(rooms) {
            Room room = this.getRoomForced(roomId);
            
            logger.trace("Room user-list startup " + roomId);
            
            room.setStatusHandle(roomStatusHandle);
            
            Iterator<String> roomUsers = room.getUsers();
            while(roomUsers.hasNext()) {
                String id = roomUsers.next();
                User user = this.getUserForced(id);
                this.sendRoomStatusEvent(user,roomId,roomStatusHandle,ENTER,SNAPSHOT);
            }

            executor.execute(new Runnable() {
                public void run() {
                    listener.onRoomListComplete(roomId, roomStatusHandle);
                }
            });
        }
    }
    
    public void startRoomChatListen(String roomId, Object roomChatHandle) {
        synchronized(rooms) {
            logger.trace("Room chat startup " + roomId);
            
            Room room = this.getRoomForced(roomId);
            
            room.setMessageHandle(roomChatHandle);
        }
        
    }

    // ROOM LIFECYCLE
        // OUT
    
    public void stopRoomListen(String roomId) {
        synchronized(rooms) {
            if (!rooms.containsKey(roomId)) {
                return;
            }
            logger.trace("Room user-list stop " + roomId);
            
            Room room = rooms.get(roomId);
            room.setStatusHandle(null);
            
            this.checkRoom(room);
        }
    }
    
    public void stopRoomChatListen(String roomId) {
        synchronized(rooms) {
            if (!rooms.containsKey(roomId)) {
                return;
            }
            logger.trace("Room chat stop " + roomId);
            
            Room room = rooms.get(roomId);
            room.setMessageHandle(null);
            
            this.checkRoom(room);
        }
        
    }
    
    // ROOM EVENTS

    //synchronized(rooms) {
    void sendRoomStatusEvent(final User user, final String roomId, final Object roomStatusHandle, boolean entering, final boolean realTimeEvent) {
        if (entering) {
            executor.execute(new Runnable() {
                public void run() {
                    listener.onUserEnter(user, roomId, roomStatusHandle, realTimeEvent);
                }
            });
        } else {
            executor.execute(new Runnable() {
                public void run() {
                    listener.onUserExit(user, roomId, roomStatusHandle);
                }
            });
        }
    }
    
    
    void sendRoomMessageEvent(final User user, final String room, final Object messageHandle, final String message) {
        final String nick = user.getNick();
        executor.execute(new Runnable() {
            public void run() {
                listener.onUserMessage(nick,message,room,messageHandle,true);
            }
        });
    }

    
    //This method is not in the original implementation --> we should use the "extra" channel
    public Object getUserStatusHandle(String id) { 
        synchronized(users) {
            if (!users.containsKey(id)) {
                return null;
            } 
            
            User user = users.get(id);
            return user.getStatusHandle();
        }
    }

    
}





