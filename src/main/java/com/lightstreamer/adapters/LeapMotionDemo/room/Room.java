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
package com.lightstreamer.adapters.LeapMotionDemo.room;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class Room {
    
    private final ChatRoom chatRoom;
    private String roomId;
    private Set<String> users = new HashSet<String>();
    private Object statusHandle = null;
    private Object messageHandle = null;
    
    public Room(ChatRoom chatRoom, String roomId) {
        this.chatRoom = chatRoom;
        this.roomId = roomId;
    }

    public String getId() {
        return this.roomId;
    }

    public void broadcastMessage(User user, String message) {
        if (this.messageHandle != null) {
          this.chatRoom.sendRoomMessageEvent(user,this.roomId,this.messageHandle,message);
        }
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
    
    void addUser(User user) {
        this.users.add(user.getId());
        if (this.statusHandle != null) {
            this.chatRoom.sendRoomStatusEvent(user, this.roomId, this.statusHandle, ChatRoom.ENTER, ChatRoom.REALTIME);
        }
    }

    void removeUser(User user) {
        if (this.users.remove(user.getId()) && this.statusHandle != null ) {
            this.chatRoom.sendRoomStatusEvent(user, this.roomId, this.statusHandle, ChatRoom.EXIT, ChatRoom.REALTIME);
        }
    }
    
    public Iterator<String> getUsers() {
        return this.users.iterator();
    }
    

}