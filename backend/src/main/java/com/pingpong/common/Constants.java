package com.pingpong.common;

public class Constants {
    
    public static final Integer SUCCESS = 200;
    public static final Integer ERROR = 500;
    public static final Integer UNAUTHORIZED = 401;
    public static final Integer FORBIDDEN = 403;
    public static final Integer NOT_FOUND = 404;
    
    public static final String USER_ROLE_USER = "USER";
    public static final String USER_ROLE_ADMIN = "ADMIN";
    
    public static final String RECOMMEND_TYPE_PERSONAL = "PERSONAL";
    public static final String RECOMMEND_TYPE_SIMILAR = "SIMILAR";
    public static final String RECOMMEND_TYPE_HOT = "HOT";
    
    public static final String CACHE_KEY_HOME_PAGE = "home_page";
    public static final String CACHE_KEY_HOT_EQUIPMENT = "hot_equipment";
    public static final String CACHE_KEY_RECOMMEND = "recommend_";
    
    public static final Integer PAGE_SIZE = 10;
    
    public static final String UPLOAD_PATH = "uploads/";
}
