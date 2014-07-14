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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class User {

    private final ChatRoom chatRoom;
    private String id;
    private String nick;
    private String statusId = "";
    private String status = "";
    
    private Map<String,String> extraProp = new HashMap<String,String>();
    
    private Object statusHandle = null;
    private Object messagesHandle = null;
    
    private Set<Room> rooms = new HashSet<Room>();
    private boolean active = false;
    
    
    User(ChatRoom chatRoom, String id) {
        this.chatRoom = chatRoom;
        this.id = id;
    }
    
    public String getId() {
        return this.id;
    }
    
    Map<String, String> getExtraProps() {
        return this.extraProp;
    }

    Object getStatusHandle() {
        return this.statusHandle;
    }

    void enterRoom(Room room) {
        room.addUser(this);
        this.rooms.add(room);
    }

    void leaveRoom(Room room) {
        room.removeUser(this);
        this.rooms.remove(room);
    }

    String getStatusId() {
        return this.statusId;
    }

    void setNick(String nick) {
        this.nick = nick;
        
        if (this.statusHandle != null) {
            this.chatRoom.sendUserStatusEvent(this, this.nick, null, null, null, this.statusHandle, ChatRoom.REALTIME);
        }
    }
    
    void setStatus(String status, String statusId) {
        this.status = status;
        this.statusId = statusId;
        
        if (this.statusHandle != null) {
            this.chatRoom.sendUserStatusEvent(this, null, this.statusId, this.status, null, this.statusHandle, ChatRoom.REALTIME);
        }
    }
    
    public synchronized void setExtraProps(Map<String,String> map) {
        extraProp.putAll(map);
        if (this.statusHandle != null) {
            this.chatRoom.sendUserStatusEvent(this, null, null, null, map, this.statusHandle, ChatRoom.REALTIME);
        }
    }
    
    boolean isListened() {
        return this.statusHandle != null || this.messagesHandle != null;
    }
    
    String getNick() {
        return this.nick;
    }

    String getStatus() {
        return this.status;
    }
    
    void setStatusHandle(Object statusHandle) {
        this.statusHandle = statusHandle;
    }

    void setHandle(Object messagesHandle) {
        this.messagesHandle = messagesHandle;
    }
    
    Iterator<Room> getRooms() {
        return rooms.iterator();
    }

    public void setActive(boolean active) {
        this.active  = active;
    }

    public boolean isActive() {
        return this.active;
    }
    

}