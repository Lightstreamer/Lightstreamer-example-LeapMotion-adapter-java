package com.lightstreamer.adapters.LeapMotionDemo;

public class Constants {

    public static String USER_SUBSCRIPTION = "user_";
    public static String ROOMPOSITION_SUBSCRIPTION = "roompos_";
    public static String ROOMCHATLIST_SUBSCRIPTION = "roomchatlist_";
    
    public static String SPLIT_CHAR_REG = "\\|";
    public static String SPLIT_CHAR = "|";
    
    public static final String LOGGER_CAT = "LS_demos_Logger.LeapDemo";
    //private static final String TRACER_CAT = "LS_LeapDemo_Logger.tracer";
    
    public static final String USER_ID = "USER_ID";
    
    public static final String NICK_MESSAGE = "nick|";
    public static final String STATUS_MESSAGE = "status|";
    public static final String VOID_STATUS_ID = "0";
    public static final String ENTER_ROOM = "enter|";
    public static final String EXIT_ROOM = "leave|";
    
    public static final String GRAB_MESSAGE = "grab|";
    public static final String RELEASE_MESSAGE = "release|";
    public static final String MOVE_MESSAGE = "move|";
    
    
    public static String getVal(String original, String type) {
        if(original.indexOf(type) == 0) {
            return original.substring(type.length());
        }
        return null;
    }
}
