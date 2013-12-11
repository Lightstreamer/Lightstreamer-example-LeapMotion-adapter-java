package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class World extends Thread {
    
    private static final int ENTER = 1;
    private static final int EXIT = 2;
    private static final int UPDATE = 3;
    
    
    private static final boolean REALTIME = true;
    private static final boolean SNAPSHOT = false;
    
    
    private Executor executor =  Executors.newSingleThreadExecutor();

    private String id;
    private UniverseListener listener;
    
    private ConcurrentHashMap<String,Player> users = new ConcurrentHashMap<String,Player>();
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
            Enumeration<Player> players = this.users.elements();
            while(players.hasMoreElements()) {
                Player player = players.nextElement();
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
        Player player = new Player(id);
        
        this.users.put(id,player); 
        if (this.handle != null) {
            this.sendPlayerStatus(id, this.id, this.handle, player, ENTER, REALTIME);
        }
    }
    

    synchronized void removeUser(String id) {
        this.users.remove(id);
        if (this.handle != null) {
            this.sendPlayerStatus(id, this.id, this.handle, null, EXIT, REALTIME);
        }
    }
    
    synchronized void armageddon() {
        this.stop = true;
    }
    
    @Override
    public void run () {
        
        while (!stop) {
                        
            Enumeration<Player> players = this.users.elements();
            while(players.hasMoreElements()) {
                Player player = players.nextElement();
                BaseModelBody boxN = player.getBody();
                
                boxN.translate(this.factorWorld);
                boxN.rotate(this.factorWorld);
                
                //sendPlayerPosition(player.getId(), this.id, this.handle, player, UPDATE, REALTIME);
            }
            
            try {
                Thread.sleep(this.frameRate);
            } catch (InterruptedException ie) {
            }
        }
    }
    
    //enter
    //exit
    //move 
    //position
    //position (forced)
    
    private synchronized void sendPlayerStatus(final String id, final String worldId, final Object worldHandle, Player player, final int updateType, final boolean isRealTime) {
        
        if (updateType == ENTER) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onPlayerCreated(id,worldId,worldHandle,isRealTime);
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
    /*
     public void sendUpdates(String user, BaseModelBody box) {
        try {
            String s = null;
            int indx = 0;
            String precision;
            boolean checkGameOver = true;

            synchronized (myWorld) {

                Enumeration<String> e = customWorlds.keys();
                while ( e.hasMoreElements()) {
                    Iterator<String> i = null;
                    
                    s = e.nextElement();
                    if ( (customWorlds.get(s)).contains(user) ) {
                        ArrayList <String> aL = worldsPrecisions.get(s);
                        if (aL != null) {
                            i = aL.iterator();
                        }
                    } 
                
            
                    if ( i == null ) {
                        if ( !user.startsWith("GhostPlayer_") ) {
                            checkGameOver = checkGameOver&&true;
                        } else {
                            checkGameOver = checkGameOver&&false;
                        }
                        
                        continue ;
                    } else {
                        checkGameOver = checkGameOver&&false;
                    }
    
                    
                    while (i.hasNext()) {
                        precision = i.next();
                        if (subscribed.contains(user+precision)) {
                            
                            if ( !user.startsWith("GhostPlayer_") ) {
                                if ( box.getInactivityPeriod() > MAX_INACTIVITY ) {
    
                                    logger.info(user + " gamed over for inactivity.");
                                
                                    Move3dMetaAdapter.terminateUser(user);
                                    
                                    if ( !myWorld.playerGameOver(user) ) {
                                        logger.warn("Game over procedure failed for " + user + " player (unknow player).");
                                        // throw new SubscriptionException("Unknow player.");
                                    }
                                
                                    return ;
                                }
                            }
                            
                            HashMap<String, String> update = new HashMap<String, String>();
                            //update.put("nick", box.getNickName());
                            //update.put("msg", box.getLastMsg());
                            
                            update.put("lifeSpan", box.getLifeSpan()+"");
    
                            if ( precision.equals("_bd") ) {
                                s = (new Base64Manager()).encodeBytes(toByteArray(box.getX()),true);
                                indx = s.indexOf("=");
                                update.put("posX", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray(box.getY()),true);
                                indx = s.indexOf("=");
                                update.put("posY", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray(box.getZ()),true);
                                indx = s.indexOf("=");
                                update.put("posZ", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray(box.getAxisAngle().toQuat().getX()),true);
                                indx = s.indexOf("=");
                                update.put("rotX", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray(box.getAxisAngle().toQuat().getY()),true);
                                indx = s.indexOf("=");
                                update.put("rotY", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray(box.getAxisAngle().toQuat().getZ()),true);
                                indx = s.indexOf("=");
                                update.put("rotZ", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray(box.getAxisAngle().toQuat().getW()),true);
                                indx = s.indexOf("=");
                                update.put("rotW", s.substring(0, indx));
                            } else if ( precision.equals("_bs") ) {
                                s = (new Base64Manager()).encodeBytes(toByteArray((float)box.getX()),true);
                                indx = s.indexOf("=");
                                update.put("posX", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray((float)box.getY()),true);
                                indx = s.indexOf("=");
                                update.put("posY", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray((float)box.getZ()),true);
                                indx = s.indexOf("=");
                                update.put("posZ", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray((float)box.getAxisAngle().toQuat().getX()),true);
                                indx = s.indexOf("=");
                                update.put("rotX", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray((float)box.getAxisAngle().toQuat().getY()),true);
                                indx = s.indexOf("=");
                                update.put("rotY", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray((float)box.getAxisAngle().toQuat().getZ()),true);
                                indx = s.indexOf("=");
                                update.put("rotZ", s.substring(0, indx));
                                s = (new Base64Manager()).encodeBytes(toByteArray((float)box.getAxisAngle().toQuat().getW()),true);
                                indx = s.indexOf("=");
                                update.put("rotW", s.substring(0, indx));
                            } else {
                                int px = 8;
                                try {
                                    px = new Integer(precision.substring(2)).intValue();
                                } catch (NumberFormatException  nfe) {
                                    logger.warn("Precision requested invalid (" + precision +") 8 assumed.");
                                }
                                update.put("posX", roundToSend(box.getX(), px));
                                update.put("posY", roundToSend(box.getY(), px));
                                update.put("posZ", roundToSend(box.getZ(), px));
                                update.put("rotX", roundToSend(box.getAxisAngle().toQuat().getX(), px));
                                update.put("rotY", roundToSend(box.getAxisAngle().toQuat().getY(), px));
                                update.put("rotZ", roundToSend(box.getAxisAngle().toQuat().getZ(), px));
                                update.put("rotW", roundToSend(box.getAxisAngle().toQuat().getW(), px));
                            }
                            
                            update.put("Vx", box.getvX()+"");
                            update.put("Vy", box.getvY()+"");
                            update.put("Vz", box.getvZ()+"");
                            update.put("momx", box.getDeltaRotX()+"");
                            update.put("momy", box.getDeltaRotY()+"");
                            update.put("momz", box.getDeltaRotZ()+"");
                            
                            if ( tracer != null ) {
                                tracer.debug("Update for " + user+precision);
                            }
                                
                            if (listener != null) {
                                listener.update(user+precision,update,false);
                            }
                        }
                    }
                }
                
                if (checkGameOver) {
                    if ( !myWorld.playerGameOver(user) ) {
                        logger.warn("Game over procedure failed for " + user + " player (unknow player).");
                        // throw new SubscriptionException("Unknow player.");
                    } else {
                        logger.info(user + " game over!");
                    }
                }
            }
        } catch (Exception e) {
            // Skip.
            logger.warn("Exception in update procedure.", e);
        }
     */
    
    private class Player {
        BaseModelBody body;
        String id;
        public Player(String id) {
            this.body = new BaseModelBody();
            this.id = id;
        }
        
        BaseModelBody getBody() {
            return this.body;
        }
        
        String getId() {
            return this.id;
        }
    }

}
