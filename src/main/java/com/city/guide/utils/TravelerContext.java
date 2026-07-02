package com.city.guide.utils;

import com.city.guide.dto.UserDTO;

public class TravelerContext {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveTraveler(UserDTO traveler){
        tl.set(traveler);
    }

    public static UserDTO getTraveler(){
        return tl.get();
    }

    public static void removeTraveler(){
        tl.remove();
    }
}
