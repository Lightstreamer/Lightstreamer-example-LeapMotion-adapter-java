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
package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.lightstreamer.adapters.LeapMotionDemo.Constants;

public class Universe {
    
    private static Logger logger = Logger.getLogger(Constants.WORLD_CAT);
    
    private Map<String,World> worlds = new HashMap<String,World>();

    
    private UniverseListener listener;
    
    public Universe(UniverseListener listener) {
        this.listener = listener;
    }

    private synchronized World getWorldForced(String id) {
        if (worlds.containsKey(id)) {
            return worlds.get(id);
            
        } else {
            World world = new World(id,listener, Constants.FRAME_INTERVAL);
            worlds.put(id, world);
            world.start();
            return world;
        }
    }

    public synchronized void removePlayerFromWorld(String id, String room) {
        if (!worlds.containsKey(room)) {
            return;
        }
        logger.info("User exiting world " + id + ": " + room);
        
        World world = worlds.get(room);
        world.removeUser(id); 
        
        this.verifyWorld(id,world);
    }

    public synchronized void addPlayerToWorld(String id, String room) {
        logger.info("User entering world " + id + ": " + room);
        
        World world = this.getWorldForced(room);
        world.addUser(id);
    }
    
    public synchronized void startWatchingWorld(String id, Object handle) {
        logger.info("Start watching world: " + id);
        
        World world = this.getWorldForced(id);
        
        world.setHandle(handle);
    }
    
    public synchronized void stopWatchingWorld(String id) {
        if (!worlds.containsKey(id)) {
            return;
        }
        logger.info("Stop watching world: " + id);
        
        World world = worlds.get(id);
        world.setHandle(null);
        
        this.verifyWorld(id,world);

    }

    private synchronized void verifyWorld(String id, World world) {
        if (world.isEmpty() && !world.isListened()) {
            logger.info("World is now useless: " + id);
            world.armageddon();
            worlds.remove(id);
        }
    }

    public void block(String playerId, String worldId) {
        if (!worlds.containsKey(worldId)) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Forwarding grab command for player" + playerId + " to world " + worldId);
        }
        World world = worlds.get(worldId);
        world.block(playerId);
    }

    public void release(String playerId, String worldId, double x, double y, double z) {
        if (!worlds.containsKey(worldId)) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Forwarding release command for player" + playerId + " to world " + worldId);
        }
        World world = worlds.get(worldId);
        world.release(playerId,x,y,z);
    }

    public void move(String playerId, String worldId, double x, double y, double z) {
        if (!worlds.containsKey(worldId)) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Forwarding move command for player" + playerId + " to world " + worldId);
        }
        World world = worlds.get(worldId);
        world.move(playerId,x,y,z);
    }
    

}
