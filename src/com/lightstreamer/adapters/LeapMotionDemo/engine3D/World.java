package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class World extends Thread {
    
    private static final int ENTER = 1;
    private static final int EXIT = 2;
    
    
    private static final boolean REALTIME = true;
    private static final boolean SNAPSHOT = false;
    
    private static final boolean NOT_FORCED = false;
    private static final boolean FORCED = true;
    
    
    private Executor executor =  Executors.newSingleThreadExecutor();

    private String id;
    private UniverseListener listener;
    
    private ConcurrentHashMap<String,BaseModelBody> users = new ConcurrentHashMap<String,BaseModelBody>();
    private Object handle = null;
    
    
    private static final int BASE_RATE = 10;
    
    private int frameRate = 10;
    private double factorWorld = 1.0;
    
    private boolean stop = false;
    
    World(String id, UniverseListener listener)  {
        this.id = id;
        this.listener = listener;
    }
    
    synchronized void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
        this.factorWorld = (double)(this.frameRate / BASE_RATE);
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
        }
    }
    
    synchronized void addUser(String id) {
        if (this.users.containsKey(id)) {
            return;
        }
        BaseModelBody player = new BaseModelBody(id);
        
        this.users.put(id,player); 
        this.sendPlayerStatus(id, this.id, this.handle, player, ENTER, REALTIME);
    }
    

    synchronized void removeUser(String id) {
        this.users.remove(id);
        this.sendPlayerStatus(id, this.id, this.handle, null, EXIT, REALTIME);
    }
    
    synchronized void armageddon() {
        this.stop = true;
    }
    
    @Override
    public void run () {
        
        while (!stop) {
                        
            Enumeration<BaseModelBody> players = this.users.elements();
            while(players.hasMoreElements()) {
                BaseModelBody player = players.nextElement();
                              
                player.translate(this.factorWorld);
                player.rotate(this.factorWorld);
                
                this.sendPlayerPosition(player.getId(), this.id, this.handle, player, NOT_FORCED);
            }
            
            try {
                Thread.sleep(this.frameRate);
            } catch (InterruptedException ie) {
            }
        }
    }
      
    private synchronized void sendPlayerStatus(final String id, final String worldId, final Object worldHandle, BaseModelBody player, final int updateType, final boolean isRealTime) {
        if (updateType == ENTER) {
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
           executor.execute(new Runnable() {
               @Override
               public void run() {
                   listener.onPlayerDisposed(id,worldId,worldHandle);
               } 
           });
        }
    }
    
    private synchronized void sendPlayerPosition(final String id, final String worldId, final Object worldHandle, BaseModelBody player, final boolean forced) {
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
        BaseModelBody player = this.users.get(playerId);
        player.block();
        this.sendPlayerAction(playerId, this.id, this.handle, player);
    }
    
    public synchronized void release(String playerId) {
        if (!this.users.containsKey(playerId)) {
            return;
        }
        BaseModelBody player = this.users.get(playerId);
        //TODO
        this.tempRandomGo(player);
        this.sendPlayerAction(playerId, this.id, this.handle, player);
    }

    public synchronized void move(String playerId) {
        if (!this.users.containsKey(playerId)) {
            return;
        }
        BaseModelBody player = this.users.get(playerId);
        //TODO
        this.tempRandomMove(player);
        
        //player.translate(this.factorWorld);
        //player.rotate(this.factorWorld);
        this.sendPlayerPosition(playerId, this.id, this.handle, player, FORCED);
    }
    
    private Random tempRandom = new Random();
    private void tempRandomGo(BaseModelBody player) {
        player.setImpulse(IBody.Axis.X, Math.round(tempRandom.nextDouble()*5));
        player.setImpulse(IBody.Axis.Y, Math.round(tempRandom.nextDouble()*5));
        player.setImpulse(IBody.Axis.Z, Math.round(tempRandom.nextDouble()*5));
        
        player.setImpulse(IBody.Axis.X, Math.round(tempRandom.nextDouble()*5));
        player.setImpulse(IBody.Axis.Y, Math.round(tempRandom.nextDouble()*5));
        player.setImpulse(IBody.Axis.Z, Math.round(tempRandom.nextDouble()*5));
        
    }
    private void tempRandomMove(BaseModelBody player) {
        player.setX(Math.round(tempRandom.nextDouble()*90));
        player.setY(Math.round(tempRandom.nextDouble()*90));
        player.setZ(Math.round(tempRandom.nextDouble()*90));
    }

}