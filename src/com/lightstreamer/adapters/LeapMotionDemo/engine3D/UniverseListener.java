package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

public interface UniverseListener {

    void onWorldComplete(String fid, Object fhandle);

    void onPlayerCreated(String id, String worldId, Object worldHandle, boolean realTimeEvent);

    void onPlayerDisposed(String id, String worldId, Object worldHandle);

}
