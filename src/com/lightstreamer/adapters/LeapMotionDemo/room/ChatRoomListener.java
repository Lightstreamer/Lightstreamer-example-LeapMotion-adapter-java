package com.lightstreamer.adapters.LeapMotionDemo.room;

public interface ChatRoomListener {

    public void onUserEnter(String id, String room, Object roomStatusHandle, boolean realTimeEvent);
    public void onRoomListComplete(String id, Object roomStatusHandle);
    public void onUserExit(String id, String room, Object roomStatusHandle);
    
    public void onUserStatusChange(String id, String nick, String statusId, String status, Object userStatusHandle, boolean realTimeEvent);
    
    public void onUserMessage(String id, String message, String room, Object roomHandle, boolean realTimeEvent); //not implemented
    public void onPrivateMessage(String fromId, String toId, String message, Object userHandle); //not implemented  
    
    public void onNewUser(String id);
    public void onUserDeleted(String id);
    
}
