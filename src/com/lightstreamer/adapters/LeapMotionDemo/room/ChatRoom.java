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
    
    public void setListener(ChatRoomListener listener) {
        this.listener = listener;
    }
    
    private User addUser(final String id) {
        synchronized(users) {
            User user = new User(id);
            user = users.put(id, user);
            
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
    

    public void startUserMessageListen(String id, Object handle) {
        synchronized(users) {
            User user;
            if (!users.containsKey(id)) {
                user = this.addUser(id);
            } else {
                user = users.get(id);
            }
            user.setHandle(handle);
        }
    }
    
    public void startUserStatusListen(String id, Object userStatusHandle) {
        synchronized(users) {
            User user = null;
            if (!users.containsKey(id)) {
                user = this.addUser(id);
            } else {
                user = users.get(id);
            }
            user.setStatusHandle(userStatusHandle);
            
            this.sendUserStatusEvent(id, user.getNick(), user.getStatusId(), user.getStatus(), userStatusHandle, SNAPSHOT);
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
                    room.removeUser(id);
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
    
    public void startRoomListen(final String roomId, final Object roomStatusHandle) {
        
        synchronized(rooms) {
            Room room = null;
            if (!rooms.containsKey(roomId)) {
                room = new Room(roomId);
                rooms.put(roomId, room);
            } else {
                room = rooms.get(roomId);
            }
            
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
            if (room.isEmpty()) {
                rooms.remove(roomId);
            }
            room.setStatusHandle(null);
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
        
        public String getStatusId() {
            return this.statusId;
        }

        public void setNick(String nick) {
            this.nick = nick;
            
            if (this.statusHandle != null) {
                sendUserStatusEvent(this.id, this.nick, this.statusId, this.status, this.statusHandle, REALTIME);
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
        
        public void setHandle(Object messageHandle) {
            this.messageHandle = messageHandle;
        }

        public void removeUser(String id) {
            this.users.remove(id);
            if (this.statusHandle != null) {
                sendRoomStatusEvent(id, this.roomId, this.statusHandle, EXIT, REALTIME);
            }
        }
        
        public Iterator<String> getUsers() {
            return this.users.iterator();
        }
        

    }

    
}





