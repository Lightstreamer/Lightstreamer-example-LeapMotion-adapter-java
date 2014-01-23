/*
  Copyright 2014 Weswit Srl

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ChatRoom {

    private ChatRoomListener listener;
    
    private static final boolean ENTER = true;
    private static final boolean EXIT = false;
    
    private static final boolean REALTIME = true;
    private static final boolean SNAPSHOT = false;
    
    
    private HashMap<String,User> users = new HashMap<String,User>();
    private HashMap<String,Room> rooms = new HashMap<String,Room>();
    
    private Executor executor =  Executors.newSingleThreadExecutor();
    
    public ChatRoom(ChatRoomListener listener) {
        this.listener = listener;
    }
    
    public User addUser(final String id) {
        synchronized(users) {
            User user = new User(id);
            users.put(id, user);
            
            executor.execute(new Runnable() {
                public void run() {
                    listener.onNewUser(id);
                }
            });
            
            return user;
        }
    }
    
    public void changeUserNick(String id, String nick) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            User user = users.get(id);
            user.setNick(nick);
        }
    }
    
    public void changeUserStatus(String id, String status, String statusId) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            User user = users.get(id);
            user.setStatus(status,statusId);
        }
    }
    
    public void enterRoom(String id, String roomId) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
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
                User user = users.get(id);
                Room room = rooms.get(roomId);
                user.leaveRoom(room);
                
                if (room.isEmpty() && !room.isListened()) {
                    rooms.remove(roomId);
                }
            }
        }
    }
    
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
    

    public void startUserMessageListen(String id, Object handle) {
        synchronized(users) {
            User user = this.getUserForced(id);
            user.setHandle(handle);
        }
    }
    
    public void startUserStatusListen(String id, Object userStatusHandle) {
        synchronized(users) {
            User user = this.getUserForced(id);
            user.setStatusHandle(userStatusHandle);
            
            this.sendUserStatusEvent(id, user.getNick(), user.getStatusId(), user.getStatus(), userStatusHandle, SNAPSHOT);
        }
    }
    
    public Object getUserStatusHandle(String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return null;
            } 
            
            User user = users.get(id);
            return user.getStatusHandle();
        }
    }

    
    public void removeUser(final String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            User user = users.remove(id);
            
            synchronized(rooms) {
                Iterator<Room> userRooms = user.getRooms();
                while(userRooms.hasNext()) {
                    Room room = userRooms.next();
                    user.leaveRoom(room);
                }
            }

            executor.execute(new Runnable() {
                public void run() {
                    listener.onUserDeleted(id);
                }
            });
        }
    }
    
    public void stopUserMessageListen(String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            User user = users.get(id);
            user.setHandle(null);
            
            if (!user.isListened()) {
                this.removeUser(id);
            }
        }
    }
    
    public void stopUserStatusListen(String id) {
        synchronized(users) {
            if (!users.containsKey(id)) {
                return;
            }
            User user = users.get(id);
            user.setStatusHandle(null);
            
            if (!user.isListened()) {
                this.removeUser(id);
            }
        }
    }
        
    //synchronized(users) {
    private void sendUserStatusEvent(final String id, final String nick, final String statusId, final String status, final Object userStatusHandle, final boolean realTimeEvent) {
        executor.execute(new Runnable() {
            public void run() {
                listener.onUserStatusChange(id, nick, statusId, status, userStatusHandle, realTimeEvent);
            }
        });
    }
    
    private Room getRoomForced(String roomId) {
        synchronized(rooms) {
            Room room = null;
            if (!rooms.containsKey(roomId)) {
                room = new Room(roomId);
                rooms.put(roomId, room);
            } else {
                room = rooms.get(roomId);
            }
            return room;
        }
    }
    
    public void startRoomListen(final String roomId, final Object roomStatusHandle) {
        
        synchronized(rooms) {
            Room room = this.getRoomForced(roomId);
            
            room.setStatusHandle(roomStatusHandle);
            
            Iterator<String> roomUsers = room.getUsers();
            while(roomUsers.hasNext()) {
                String id = roomUsers.next();
                this.sendRoomStatusEvent(id,roomId,roomStatusHandle,ENTER,SNAPSHOT);
            }

            executor.execute(new Runnable() {
                public void run() {
                    listener.onRoomListComplete(roomId, roomStatusHandle);
                }
            });
        }
    }
    
    public void stopRoomListen(String roomId) {
        synchronized(rooms) {
            if (!rooms.containsKey(roomId)) {
                return;
            }
            Room room = rooms.get(roomId);
            room.setStatusHandle(null);
            if (room.isEmpty() && !room.isListened()) {
                rooms.remove(roomId);
            }
        }
    }

    //synchronized(rooms) {
    private void sendRoomStatusEvent(final String id, final String roomId, final Object roomStatusHandle, boolean entering, final boolean realTimeEvent) {
        if (entering) {
            executor.execute(new Runnable() {
                public void run() {
                    listener.onUserEnter(id, roomId, roomStatusHandle, realTimeEvent);
                }
            });
        } else {
            executor.execute(new Runnable() {
                public void run() {
                    listener.onUserExit(id, roomId, roomStatusHandle);
                }
            });
        }
    }
    
   
    
    private class User {

        private String id;
        private String nick;
        private String statusId = "";
        private String status = "";
        
        private Object statusHandle = null;
        private Object messagesHandle = null;
        
        private Set<Room> rooms = new HashSet<Room>();
        
        
        public User(String id) {
            this.id = id;
        }
        
        public Object getStatusHandle() {
            return this.statusHandle;
        }

        public void enterRoom(Room room) {
            room.addUser(this.id);
            this.rooms.add(room);
        }

        public void leaveRoom(Room room) {
            room.removeUser(this.id);
            this.rooms.remove(room);
        }

        public String getStatusId() {
            return this.statusId;
        }

        public void setNick(String nick) {
            this.nick = nick;
            
            if (this.statusHandle != null) {
                sendUserStatusEvent(this.id, this.nick, null, null, this.statusHandle, REALTIME);
            }
        }
        
        public void setStatus(String status, String statusId) {
            this.status = status;
            this.statusId = statusId;
            
            if (this.statusHandle != null) {
                sendUserStatusEvent(this.id, null, this.statusId, this.status, this.statusHandle, REALTIME);
            }
        }
        
        public boolean isListened() {
            return this.statusHandle != null || this.messagesHandle != null;
        }
        
        public String getNick() {
            return this.nick;
        }

        public String getStatus() {
            return this.status;
        }
        
        public void setStatusHandle(Object statusHandle) {
            this.statusHandle = statusHandle;
        }

        public void setHandle(Object messagesHandle) {
            this.messagesHandle = messagesHandle;
        }
        
        public Iterator<Room> getRooms() {
            return rooms.iterator();
        }
        
       
    }
    
    private class Room {
        
        private String roomId;
        private Set<String> users = new HashSet<String>();
        private Object statusHandle = null;
        private Object messageHandle = null;
        
        public Room(String roomId) {
            this.roomId = roomId;
        }
       
        public boolean isEmpty() {
            return users.isEmpty();
        }
        
        public boolean isListened() {
            return this.statusHandle != null || this.messageHandle != null;
        }

        public void setStatusHandle(Object roomStatusHandle) {
            this.statusHandle = roomStatusHandle;
        }
        
        public void setMessageHandle(Object messageHandle) {
            this.messageHandle = messageHandle;
        }
        
        void addUser(String id) {
            this.users.add(id);
            if (this.statusHandle != null) {
                sendRoomStatusEvent(id, this.roomId, this.statusHandle, ENTER, REALTIME);
            }
        }

        void removeUser(String id) {
            if (this.users.remove(id) && this.statusHandle != null ) {
                sendRoomStatusEvent(id, this.roomId, this.statusHandle, EXIT, REALTIME);
            }
        }
        
        public Iterator<String> getUsers() {
            return this.users.iterator();
        }
        

    }

    
}





