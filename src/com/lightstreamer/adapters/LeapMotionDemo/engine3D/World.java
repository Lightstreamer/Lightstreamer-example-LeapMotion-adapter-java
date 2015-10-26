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
package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.lightstreamer.adapters.LeapMotionDemo.Constants;

public class World extends Thread {
    
    private static final int ENTER = 1;
    private static final int EXIT = 2;
    
    
    private static final boolean REALTIME = true;
    private static final boolean SNAPSHOT = false;
    
    private static final boolean NOT_FORCED = false;
    private static final boolean FORCED = true;
    
    private Logger logger = Logger.getLogger(Constants.WORLD_CAT);
    
    private Executor executor =  Executors.newSingleThreadExecutor();

    private String id;
    private UniverseListener listener;
    
    private ConcurrentHashMap<String,BaseModelBody> users = new ConcurrentHashMap<String,BaseModelBody>();
    private Object handle = null;
    
    private int frameInterval = 0;
    private double factorWorld = 1.0;
    
    private boolean stop = false;
    
    World(String id, UniverseListener listener, int frameInterval)  {
        this.id = id;
        this.listener = listener;
        
        this.frameInterval = frameInterval;
        this.factorWorld = (double)(this.frameInterval / Constants.BASE_RATE);
        
        logger.info("A new world is born: " + id + " ("+this.frameInterval+")");
    }
    
    synchronized void setFrameInterval(int frameInterval) {
        logger.info(this.id+"|Frame interval changed: " + frameInterval);
        this.frameInterval = frameInterval;
        this.factorWorld = (double)(this.frameInterval / Constants.BASE_RATE);
    }

    synchronized boolean isListened() {
        return this.handle != null;
    }
    
    synchronized boolean isEmpty() {
        return users.isEmpty();
    }
    
    synchronized void setHandle(Object handle) {
        
        this.handle = handle;
        
        if (this.handle != null) {
            logger.info(this.id+"|New handle set");
            logger.debug(this.id+"|preparing snapshot events");
            
            Enumeration<BaseModelBody> players = this.users.elements();
            while(players.hasMoreElements()) {
                BaseModelBody player = players.nextElement();
                this.sendPlayerStatus(player.getId(), this.id, this.handle, player, ENTER, SNAPSHOT);
            }
            
            final String fid = this.id;
            final Object fhandle = this.handle;
            executor.execute(new Runnable() {
                public void run() {
                    listener.onWorldComplete(fid,fhandle);
                }
            });
            
        } else {
            logger.info(this.id+"|Handle removed");
        }
    }
    
    synchronized void addUser(String id) {
        if (this.users.containsKey(id)) {
            return;
        }
        BaseModelBody player = new BaseModelBody(id);
        
        this.users.put(id,player); 
        logger.info(this.id+"|A new user entered" + id);
        
        this.sendPlayerStatus(id, this.id, this.handle, player, ENTER, REALTIME);
        
    }
    

    synchronized void removeUser(String id) {
        this.users.remove(id);
        logger.info(this.id+"|A user left" + id);
        
        this.sendPlayerStatus(id, this.id, this.handle, null, EXIT, REALTIME);
    }
    
    synchronized void armageddon() {
        this.stop = true;
        logger.info(this.id+"|World ended");
    }
    
    @Override
    public void run () {
        
        while (!stop) {
            
            if (logger.isTraceEnabled()) {
                logger.trace(this.id+"|generating frame");
            }
                        
            Enumeration<BaseModelBody> players = this.users.elements();
            while(players.hasMoreElements()) {
                BaseModelBody player = players.nextElement();
                              
                player.translate(this.factorWorld);
                player.rotate(this.factorWorld);
                
                this.sendPlayerPosition(player.getId(), this.id, this.handle, player, NOT_FORCED);
            }
            
            if (logger.isTraceEnabled()) {
                logger.trace(this.id+"|frame generated");
            }
            
            try {
                Thread.sleep(this.frameInterval);
            } catch (InterruptedException ie) {
            }
        }
    }
      
    private synchronized void sendPlayerStatus(final String id, final String worldId, final Object worldHandle, BaseModelBody player, final int updateType, final boolean isRealTime) {
        if (updateType == ENTER) {
            logger.debug(this.id+"|Preparing enter event for " + id);
            
            final HashMap<String,String> currentPosition = new HashMap<String,String>();
            player.fillPositionMap(currentPosition);
            
            final HashMap<String,String> currentImpulses = new HashMap<String,String>();
            player.fillImpulseMap(currentImpulses);
            
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onPlayerCreated(id,worldId,worldHandle,currentPosition,currentImpulses,isRealTime);
                } 
            });
        } else if (updateType == EXIT) {
           logger.debug(this.id+"|Preparing exit event for " + id);
            
           executor.execute(new Runnable() {
               @Override
               public void run() {
                   listener.onPlayerDisposed(id,worldId,worldHandle);
               } 
           });
        }
    }
    
    private synchronized void sendPlayerPosition(final String id, final String worldId, final Object worldHandle, BaseModelBody player, final boolean forced) {
        if (logger.isTraceEnabled()) {
            logger.trace(this.id+"|Preparing position event for " + id);
        }
        
        
        final HashMap<String, String> currentPosition = new HashMap<String, String>();
        player.fillPositionMap(currentPosition);
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onPlayerMoved(id, worldId, worldHandle, currentPosition, forced);
            } 
        });
    }
    
    private synchronized void sendPlayerAction(final String id, final String worldId, final Object worldHandle, BaseModelBody player) {
        if (logger.isTraceEnabled()) {
            logger.trace(this.id+"|Preparing action event for " + id);
        }
        
        final HashMap<String, String> currentImpulses = new HashMap<String, String>();
        player.fillImpulseMap(currentImpulses);
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onPlayerActed(id, worldId, worldHandle, currentImpulses);
            } 
        });
    }

    public synchronized void block(String playerId) {
        if (!this.users.containsKey(playerId)) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace(this.id+"|blocking " + id);
        }
        BaseModelBody player = this.users.get(playerId);
        player.block();
        this.sendPlayerAction(playerId, this.id, this.handle, player);
    }
    
    public synchronized void release(String playerId, double x, double y, double z) {
        if (!this.users.containsKey(playerId)) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace(this.id+"|releasing " + id);
        }
        BaseModelBody player = this.users.get(playerId);
        player.setImpulse(IBody.Axis.X, x);
        player.setImpulse(IBody.Axis.Y, y);
        player.setImpulse(IBody.Axis.Z, z);
        
        player.setTourque(IBody.Axis.X,  Math.round(x/2));
        player.setTourque(IBody.Axis.Y,  Math.round(y/2));
        player.setTourque(IBody.Axis.Z,  Math.round(z/2));
     
        this.sendPlayerAction(playerId, this.id, this.handle, player);
    }

    public synchronized void move(String playerId, double x, double y, double z) {
        if (!this.users.containsKey(playerId)) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace(this.id+"|moving " + id);
        }
        BaseModelBody player = this.users.get(playerId);
        
        player.setX(x);
        player.setY(y);
        player.setZ(z);
       
        this.sendPlayerPosition(playerId, this.id, this.handle, player, FORCED);
    }

}
