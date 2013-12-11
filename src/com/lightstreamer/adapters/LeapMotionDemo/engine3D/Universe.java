package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.lightstreamer.adapters.LeapMotionDemo.Constants;

public class Universe {
    
    public static Logger logger = Logger.getLogger(Constants.LOGGER_CAT);
    
    private Map<String,World> worlds = new HashMap<String,World>();

    
    private UniverseListener listener;
    
    public Universe(UniverseListener listener) {
        this.listener = listener;
    }

    private synchronized World getWorldForced(String id) {
        if (worlds.containsKey(id)) {
            return worlds.get(id);
            
        } else {
            World world = new World(id,listener);
            worlds.put(id, world);
            world.start();
            return world;
        }
    }

    public synchronized void removePlayerFromWorld(String id, String room) {
        if (!worlds.containsKey(id)) {
            return;
        }
        World world = worlds.get(id);
        world.removeUser(id); 
        
        this.verifyWorld(id,world);
    }

    public synchronized void addPlayerToWorld(String id, String room) {
        World world = this.getWorldForced(room);
        world.addUser(id);
    }
    
    public synchronized void startWatchingWorld(String id, Object handle) {
        World world = this.getWorldForced(id);
        
        world.setHandle(handle);
    }
    
    public synchronized void stopWatchingWorld(String id) {
        if (!worlds.containsKey(id)) {
            return;
        }
        World world = worlds.get(id);
        world.setHandle(null);
        
        this.verifyWorld(id,world);

    }

    private synchronized void verifyWorld(String id, World world) {
        if (world.isEmpty() && !world.isListened()) {
            world.armageddon();
            worlds.remove(id);
        }
    }
    

}
