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
package com.lightstreamer.adapters.LeapMotionDemo;

public class Constants {

    public static String USER_SUBSCRIPTION = "user_";
    public static String ROOMPOSITION_SUBSCRIPTION = "roompos_";
    public static String ROOMCHATLIST_SUBSCRIPTION = "roomchatlist_";
    
    public static String SPLIT_CHAR_REG = "\\|";
    public static String SPLIT_CHAR = "|";
    
    public static final String LOGGER_CAT = "LS_demos_Logger.LeapDemo.adapters";
    public static final String CHAT_CAT = "LS_demos_Logger.LeapDemo.chat";
    public static final String WORLD_CAT = "LS_demos_Logger.LeapDemo.world";
    
    public static final String USER_ID = "USER_ID";
    
    public static final String NICK_MESSAGE = "nick|";
    public static final String STATUS_MESSAGE = "status|";
    public static final String VOID_STATUS_ID = "0";
    public static final String ENTER_ROOM = "enter|";
    public static final String EXIT_ROOM = "leave|";
    
    public static final String GRAB_MESSAGE = "grab|";
    public static final String RELEASE_MESSAGE = "release|";
    public static final String MOVE_MESSAGE = "move|";
    
    public static final int BASE_RATE = 10;
    public static final int FRAME_INTERVAL = 50;
    
    
    public static String getVal(String original, String type) {
        if(original.indexOf(type) == 0) {
            return original.substring(type.length());
        }
        return null;
    }
}
