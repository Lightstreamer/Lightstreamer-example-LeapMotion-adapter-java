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

import java.util.Map;

public interface ChatRoomListener {

    public void onUserEnter(User user, String room, Object roomStatusHandle, boolean realTimeEvent);
    public void onRoomListComplete(String id, Object roomStatusHandle);
    public void onUserExit(User user, String room, Object roomStatusHandle);
    
    public void onUserStatusChange(User user, String nick, String statusId, String status, Map<String,String> extra, Object userStatusHandle, boolean realTimeEvent);
    
    public void onUserMessage(String user, String message, String room, Object roomHandle, boolean realTimeEvent);
    public void onPrivateMessage(String fromId, String toId, String message, Object userHandle); //not implemented  
    
    public void onNewUser(String id);
    public void onUserDeleted(String id);

    
}
