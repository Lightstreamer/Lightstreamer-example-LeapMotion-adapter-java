package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

import java.util.HashMap;

public interface UniverseListener {

    void onWorldComplete(String fid, Object fhandle);

    void onPlayerCreated(String id, String worldId, Object worldHandle, HashMap<String,String> currentPosition,  HashMap<String,String> currentImpulses, boolean realTimeEvent);

    void onPlayerDisposed(String id, String worldId, Object worldHandle);
    
    void onPlayerMoved(String id, String worldId, Object worldHandle,  HashMap<String,String> currentPosition, boolean forced);
    
    void onPlayerActed(String id, String worldId, Object worldHandle,  HashMap<String,String> currentImpulses);

}
